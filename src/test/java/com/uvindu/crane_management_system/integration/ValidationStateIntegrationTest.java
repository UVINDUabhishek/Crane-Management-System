package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ValidationStateIntegrationTest extends AbstractIntegrationTest {
    @Autowired PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @EnumSource(value = JobCardStatus.class, names = {"COMPLETED", "CANCELED"})
    void terminalJobsAndTheirTimesheetsAreReadOnly(JobCardStatus state) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Timesheet sheet = fixtures.fixtureTimesheet(job);
        mockMvc.perform(authorized(patch("/api/job-card/{id}/" + (state == JobCardStatus.COMPLETED ? "complete" : "cancel"), job.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())), body(job, "2026-11-10", "\"2026-11-12\"")))
                .andExpect(status().isConflict());
        for (String action : List.of("complete", "cancel")) {
            mockMvc.perform(authorized(patch("/api/job-card/{id}/" + action, job.getId()))).andExpect(status().isConflict());
        }
        String sheetBody = """
                {"jobCardId":%d,"dateOfWork":"2020-01-02","workedHours":9,"description":"Changed"}
                """.formatted(job.getId());
        mockMvc.perform(json(authorized(post("/api/timesheets")), sheetBody)).andExpect(status().isConflict());
        mockMvc.perform(json(authorized(put("/api/timesheets/{id}", sheet.getId())), sheetBody)).andExpect(status().isConflict());
        mockMvc.perform(authorized(delete("/api/timesheets/{id}", sheet.getId()))).andExpect(status().isConflict());
        assertThat(timesheetRepository.findById(sheet.getId()).orElseThrow().getWorkedHours()).isEqualByComparingTo("5");
        assertThat(timesheetRepository.count()).isEqualTo(1);
        assertThat(invoiceRepository.count()).isEqualTo(state == JobCardStatus.COMPLETED ? 1 : 0);
        if (state == JobCardStatus.COMPLETED) {
            Invoice invoice = invoiceRepository.findAll().getFirst();
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
            assertThat(invoice.getAmount()).isEqualByComparingTo("8500");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer", "type", "before", "after", "open", "inactiveCustomer", "inactiveCrane", "inactiveQuote", "missingValidFrom", "missingValidTo"})
    void incompatibleQuotationsAreRejectedOnCreateAndUpdate(String violation) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Quotation quote = job.getQuotationId();
        String start = "2026-11-10", end = "\"2026-11-12\"";
        switch (violation) {
            case "customer" -> { quote.setCustomerId(fixtures.customer("OTHER")); quotationRepository.save(quote); }
            case "type" -> { quote.setType("OTHER"); quotationRepository.save(quote); }
            case "before" -> start = "2026-09-30";
            case "after" -> end = "\"2027-01-01\"";
            case "open" -> end = "null";
            case "missingValidFrom" -> { quote.setValidFrom(null); quotationRepository.save(quote); }
            case "missingValidTo" -> { quote.setValidTo(null); quotationRepository.save(quote); }
            case "inactiveCustomer" -> { Customer customer = job.getCustomerId(); customer.setActive(false); customerRepository.save(customer); }
            case "inactiveCrane" -> { Crane crane = job.getCraneId(); crane.setActive(false); craneRepository.save(crane); }
            case "inactiveQuote" -> { quote.setActive(false); quotationRepository.save(quote); }
        }
        int expected = violation.startsWith("inactive") ? 409 : 400;
        mockMvc.perform(json(authorized(post("/api/job-card")), body(job, start, end))).andExpect(status().is(expected));
        mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())), body(job, start, end))).andExpect(status().is(expected));
        JobCard unchanged = jobCardRepository.findById(job.getId()).orElseThrow();
        assertThat(unchanged.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(unchanged.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(jobCardRepository.count()).isEqualTo(1);
        mockMvc.perform(authorized(get("/api/job-card/{id}", job.getId()))).andExpect(status().isOk());
    }

    @Test
    void quotationValidityBoundariesAreInclusiveAndOmittedJobEndIsPreserved() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())), body(job, "2026-10-01", "\"2026-12-31\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endDate").value("2026-12-31"));
        String omitted = body(job, "2026-10-01", "null").replace(",\"endDate\":null", "");
        mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())), omitted))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endDate").value("2026-12-31"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"customerId", "quotationId", "craneId", "operatorId"})
    void missingJobRelationshipsReturn404(String field) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        String payload = body(job, "2026-11-10", "\"2026-11-12\"");
        if (field.equals("operatorId")) payload = payload.replace("{", "{\"operatorId\":99999,");
        else payload = payload.replaceAll("\"" + field + "\":\\d+", "\"" + field + "\":99999");
        mockMvc.perform(json(authorized(post("/api/job-card")), payload)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"email\":\"invalid\",\"password\":\"top-secret-value\"}", "{\"email\":\"admin@test.local\",\"password\":\" \"}"})
    void malformedOrInvalidLoginReturnsSanitized400(String payload) throws Exception {
        String response = mockMvc.perform(json(post("/api/auth/login"), payload)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").exists()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("top-secret-value", "Exception", "java.");
    }

    @Test
    void securityFailuresUseConsistentProblemDetails() throws Exception {
        problem(mockMvc.perform(get("/api/auth/me")), 401, "UNAUTHORIZED");
        problem(mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer broken")), 401, "UNAUTHORIZED");
        problem(mockMvc.perform(json(post("/api/auth/login"), """
                {"email":"admin@test.local","password":"wrong"}
                """)), 401, "UNAUTHORIZED");
        problem(mockMvc.perform(asRole(get("/api/admin/users"), "sales@test.local")), 403, "FORBIDDEN");
        problem(mockMvc.perform(asRole(get("/api/invoices"), "sales@test.local")), 403, "FORBIDDEN");
        String jwt = token("sales@test.local");
        User sales = userRepository.findByEmail("sales@test.local").orElseThrow();
        sales.setActive(false); userRepository.save(sales);
        problem(mockMvc.perform(get("/api/auth/me").header("Authorization", jwt)), 401, "UNAUTHORIZED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"users", "users/1", "me/password"})
    void adminRequestsAreValidatedBeforeMutation(String path) throws Exception {
        mockMvc.perform(json(authorized(path.equals("users") ? post("/api/admin/" + path) : put("/api/admin/" + path)), "{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").exists()).andExpect(jsonPath("$.errors[0].message").exists())
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        assertThat(userRepository.count()).isEqualTo(4);
    }

    @Test
    void invalidEnumsAndInvalidPathTypesAre400() throws Exception {
        problem(mockMvc.perform(authorized(get("/api/job-card/not-a-number"))), 400, "INVALID_REQUEST");
        problem(mockMvc.perform(json(authorized(post("/api/admin/users")), """
                {"username":"test","email":"test@example.com","password":"secret","role":"SUPERUSER"}
                """)), 400, "INVALID_REQUEST");
    }

    @Test
    void databaseDuplicateIs409WithoutSqlOrPasswordDetails() throws Exception {
        String response = mockMvc.perform(json(authorized(post("/api/admin/users")), """
                {"username":"duplicate","email":"admin@test.local","password":"do-not-echo","role":"SALES"}
                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("do-not-echo", "insert", "constraint", "$2", "Exception");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null"})
    void invoiceRevisionValidatesRequiredPositiveNumbers(String qty) throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(authorized(patch("/api/invoices/{id}/complete", invoice.getId()))).andExpect(status().isOk());
        mockMvc.perform(json(authorized(patch("/api/invoices/{id}/revise", invoice.getId())),
                        "{\"qty\":" + qty + ",\"rate\":100,\"mobilization\":0,\"description\":\"revision\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(invoiceRepository.count()).isEqualTo(1);
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus()).isEqualTo(InvoiceStatus.COMPLETED);
    }

    @Test
    void quotationDatesAndOptionalNumericIdsAreValidated() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        String quote = """
                {"customerId":%d,"type":"001","validFrom":"2026-12-31","validTo":"2026-01-01",
                 "minHours":8,"minRate":1000,"mobilization":500,"description":"Invalid dates"}
                """.formatted(job.getCustomerId().getId());
        mockMvc.perform(json(authorized(post("/api/quotations")), quote)).andExpect(status().isBadRequest());
        mockMvc.perform(json(authorized(put("/api/quotations/{id}", job.getQuotationId().getId())), quote)).andExpect(status().isBadRequest());
        String maintenance = fixtures.maintenanceBody(job.getCraneId().getId()).replace("{", "{\"maintenancePersonId\":-1,");
        mockMvc.perform(json(authorized(post("/api/maintenance")), maintenance)).andExpect(status().isBadRequest());
        mockMvc.perform(json(authorized(post("/api/timesheets")), """
                {"jobCardId":-1,"dateOfWork":"2020-01-01","workedHours":1}
                """ )).andExpect(status().isBadRequest());
    }

    @Test
    void concurrentCompletionCreatesExactlyOneDraftInvoice() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        fixtures.fixtureTimesheet(job);
        Callable<Integer> complete = () -> mockMvc.perform(authorized(patch("/api/job-card/{id}/complete", job.getId())))
                .andReturn().getResponse().getStatus();
        List<Integer> results = raceOnJob(job.getId(), complete, complete);
        assertThat(results).containsExactlyInAnyOrder(200, 409);
        assertThat(invoiceRepository.count()).isEqualTo(1);
        assertThat(invoiceRepository.findAll().getFirst().getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void concurrentTimesheetCreationAndCompletionCannotChangeBilledHoursAfterCompletion() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        fixtures.fixtureTimesheet(job);
        List<Integer> results = raceOnJob(job.getId(),
                () -> mockMvc.perform(authorized(patch("/api/job-card/{id}/complete", job.getId()))).andReturn().getResponse().getStatus(),
                () -> mockMvc.perform(json(authorized(post("/api/timesheets")), """
                        {"jobCardId":%d,"dateOfWork":"2020-01-02","workedHours":10}
                        """.formatted(job.getId()))).andReturn().getResponse().getStatus());
        assertThat(results.getFirst()).isEqualTo(200);
        assertThat(results.get(1)).isIn(201, 409);
        JobCard completed = jobCardRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getTotalWorkedHours()).isEqualByComparingTo(results.get(1) == 201 ? "15" : "5");
        assertThat(completed.getTotalBillableHours()).isEqualByComparingTo(results.get(1) == 201 ? "18" : "8");
        assertThat(invoiceRepository.findAll().getFirst().getQty()).isEqualByComparingTo(completed.getTotalBillableHours());
    }

    private List<Integer> raceOnJob(Long id, Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> requests = new TransactionTemplate(transactionManager).execute(transaction -> {
                jobCardRepository.findByIdForUpdate(id).orElseThrow();
                List<Future<Integer>> futures = List.of(executor.submit(first), executor.submit(second));
                await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and query like '%from job_card%'",
                        Integer.class)).isEqualTo(2));
                return futures;
            });
            return List.of(requests.get(0).get(20, TimeUnit.SECONDS), requests.get(1).get(20, TimeUnit.SECONDS));
        }
    }
    private String body(JobCard job, String start, String end) {
        return """
                {"customerId":%d,"quotationId":%d,"craneId":%d,"startDate":"%s","endDate":%s,
                "location":"Changed","description":"Validation test"}
                """.formatted(job.getCustomerId().getId(), job.getQuotationId().getId(), job.getCraneId().getId(), start, end);
    }
    private void problem(ResultActions result, int statusCode, String code) throws Exception {
        result.andExpect(status().is(statusCode)).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(statusCode)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.startsWith("urn:chms:problem:"))).andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }
}
