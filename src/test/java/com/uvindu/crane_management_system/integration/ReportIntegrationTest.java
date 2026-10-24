package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportIntegrationTest extends AbstractIntegrationTest {

    @Test void emptyJobCardReportReturnsArray() throws Exception {
        mockMvc.perform(authorized(get("/api/reports/job-cards")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void jobCardReportContainsPersistedJob() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(authorized(get("/api/reports/job-cards")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobCardNumber").value(job.getJobCardNumber()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test void filterJobsByCustomerReturnsMatchingJob() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(authorized(get("/api/reports/job-cards/customer/{id}", job.getCustomerId().getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(authorized(get("/api/reports/job-cards/customer/{id}", 999999L)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void filterJobsByStatusReturnsOnlyMatchingRecords() throws Exception {
        fixtures.fixtureJobCard();
        mockMvc.perform(authorized(get("/api/reports/job-cards/status/PENDING")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(authorized(get("/api/reports/job-cards/status/COMPLETED")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void salesCanViewJobReportsButNotInvoiceReports() throws Exception {
        mockMvc.perform(asRole(get("/api/reports/job-cards"), "sales@test.local"))
                .andExpect(status().isOk());
        mockMvc.perform(asRole(get("/api/reports/invoices"), "sales@test.local"))
                .andExpect(status().isForbidden());
    }

    @Test void financeCanViewInvoiceReports() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(asRole(get("/api/reports/invoices"), "finance@test.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value(invoice.getInvoiceNumber()));
    }

    @Test void invoiceReportFiltersByCustomer() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(authorized(get("/api/reports/invoices/customer/{id}", invoice.getCustomer().getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void craneUtilizationIncludesTimesheetWork() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        fixtures.fixtureTimesheet(job);
        mockMvc.perform(authorized(get("/api/reports/crane-utilization")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].craneNumber").value(job.getCraneId().getCraneNumber()));
    }

    @Test void operationsCannotViewRevenueMonthlyReport() throws Exception {
        mockMvc.perform(asRole(get("/api/reports/revenue/monthly"), "operations@test.local"))
                .andExpect(status().isForbidden());
    }

    @Test void revenueAndMaintenanceReportsRespondForAdmin() throws Exception {
        mockMvc.perform(authorized(get("/api/reports/revenue-summary"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/revenue/monthly"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/revenue/customer"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/revenue/crane"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/maintenance"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/maintenance/status"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/maintenance/summary"))).andExpect(status().isOk());
    }

    @Test void unauthenticatedReportRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/reports/job-cards")).andExpect(status().isUnauthorized());
    }

    @Test
    void revenueReportsCountMobilizationOnceAcrossInvoiceLifecycleWithoutRewritingHistory() throws Exception {
        Invoice original = fixtures.fixtureInvoice(); // 8 billable hours * 1000 + 500 mobilization
        assertRevenueReports(0, 0, 0);
        mockMvc.perform(asRole(patch("/api/invoices/{id}/complete", original.getId()), "finance@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(8500));
        assertRevenueReports(1, 8500, 500);
        mockMvc.perform(asRole(get("/api/reports/invoices"), "finance@test.local"))
                .andExpect(jsonPath("$[0].amount").value(8500))
                .andExpect(jsonPath("$[0].mobilization").value(500));

        String revised = mockMvc.perform(json(asRole(patch("/api/invoices/{id}/revise", original.getId()),
                        "finance@test.local"),
                        "{\"qty\":10,\"rate\":1200,\"mobilization\":300,\"description\":\"Revised hire\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(12300))
                .andReturn().getResponse().getContentAsString();
        long revisedId = ((Number) JsonPath.read(revised, "$.id")).longValue();
        assertRevenueReports(0, 0, 0); // REVISED original and DRAFT replacement are excluded.
        mockMvc.perform(asRole(patch("/api/invoices/{id}/complete", revisedId), "finance@test.local"))
                .andExpect(status().isOk());
        assertRevenueReports(1, 12300, 300);
        Invoice historical = invoiceRepository.findById(original.getId()).orElseThrow();
        assertThat(historical.getAmount()).isEqualByComparingTo("8500");
        assertThat(historical.getMobilization()).isEqualByComparingTo("500");
        Invoice replacement = invoiceRepository.findById(revisedId).orElseThrow();
        assertThat(replacement.getAmount()).isEqualByComparingTo("12300");
        assertThat(replacement.getMobilization()).isEqualByComparingTo("300");
        mockMvc.perform(asRole(patch("/api/invoices/{id}/cancel", revisedId), "finance@test.local"))
                .andExpect(status().isOk());
        assertRevenueReports(0, 0, 0); // CANCELED invoices remain excluded.
    }

    @Test
    void maintenanceReportIncludesAssignedAndUnassignedRecords() throws Exception {
        Crane crane = fixtures.fixtureCrane();
        String unassigned = fixtures.maintenanceBody(crane.getId());
        mockMvc.perform(json(authorized(post("/api/maintenance")), unassigned))
                .andExpect(status().isCreated());
        User assignee = userRepository.findByEmail("operations@test.local").orElseThrow();
        String assigned = unassigned.replace("}", ",\"maintenancePersonId\":" + assignee.getId() + "}");
        mockMvc.perform(json(authorized(post("/api/maintenance")), assigned))
                .andExpect(status().isCreated());

        String report = mockMvc.perform(asRole(get("/api/reports/maintenance"), "operations@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        java.util.List<java.util.Map<String, Object>> rows = JsonPath.read(report, "$");
        var noAssignee = rows.stream().filter(row -> row.get("maintenanceNumber").equals("MT-0001"))
                .findFirst().orElseThrow();
        var withAssignee = rows.stream().filter(row -> row.get("maintenanceNumber").equals("MT-0002"))
                .findFirst().orElseThrow();
        assertThat(noAssignee).containsEntry("maintenancePerson", null);
        assertThat(withAssignee).containsEntry("maintenancePerson", assignee.getUsername());
        for (var row : rows) {
            assertThat(row).containsEntry("craneNumber", crane.getCraneNumber())
                    .containsEntry("repairType", "PREVENTIVE").containsEntry("status", "SCHEDULED")
                    .containsEntry("startDate", "2026-11-15").containsEntry("endDate", "2026-11-16");
        }
    }

    private void assertRevenueReports(int count, int amount, int mobilization) throws Exception {
        String dateQuery = "?fromDate=" + LocalDate.now().minusDays(1) + "&toDate=" + LocalDate.now().plusDays(1);
        for (String path : new String[]{"/api/reports/revenue-summary", "/api/reports/revenue-summary/date" + dateQuery}) {
            mockMvc.perform(asRole(get(path), "finance@test.local"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalInvoices").value(count))
                    .andExpect(jsonPath("$.totalAmount").value(amount))
                    .andExpect(jsonPath("$.totalMobilization").value(mobilization))
                    .andExpect(jsonPath("$.totalRevenue").value(amount));
        }
        for (String grouping : new String[]{"monthly", "customer", "crane"}) {
            var result = mockMvc.perform(asRole(get("/api/reports/revenue/" + grouping), "finance@test.local"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(count == 0 ? 0 : 1));
            if (count > 0) {
                result.andExpect(jsonPath("$[0].totalInvoices").value(count))
                        .andExpect(jsonPath("$[0].totalAmount").value(amount))
                        .andExpect(jsonPath("$[0].totalMobilization").value(mobilization))
                        .andExpect(jsonPath("$[0].totalRevenue").value(amount));
            }
        }
    }
}
