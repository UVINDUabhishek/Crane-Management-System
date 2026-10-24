package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

class JobCardIntegrationTest extends AbstractIntegrationTest {
    @Test void createsPendingJobAndPersistsRelationships() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        assertThat(job.getStatus()).isEqualTo(JobCardStatus.PENDING);
        assertThat(job.getCustomerId().getRegistrationNo()).isEqualTo("BATCH2-CUST");
        assertThat(job.getSalesPersonId().getEmail()).isEqualTo("admin@test.local");
        mockMvc.perform(authorized(get("/api/job-card/{id}", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    }
    @Test void listsCreatedJob() throws Exception {
        fixtures.fixtureJobCard();
        mockMvc.perform(authorized(get("/api/job-card")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"));
    }
    @Test void rejectsInvalidCreatePayload() throws Exception {
        mockMvc.perform(json(authorized(post("/api/job-card")), "{}"))
                .andExpect(status().isBadRequest());
        assertThat(jobCardRepository.count()).isZero();
    }

    @Test
    void operationsCannotCreateJob() throws Exception {

        Customer customer = fixtures.customer("RBAC-JOB-CUST");
        Crane crane = fixtures.fixtureCrane();
        Quotation quotation = fixtures.fixtureQuotation(customer);

        String requestBody = """
            {
                "customerId": %d,
                "quotationId": %d,
                "craneId": %d,
                "startDate": "2026-11-10",
                "endDate": "2026-11-12",
                "location": "Colombo",
                "description": "RBAC integration test"
            }
            """.formatted(
                customer.getId(),
                quotation.getId(),
                crane.getId()
        );

        mockMvc.perform(
                        json(
                                asRole(
                                        post("/api/job-card"),
                                        "operations@test.local"
                                ),
                                requestBody
                        )
                )
                .andExpect(status().isForbidden());

        // Unauthorized requests must not create a job card.
        assertThat(jobCardRepository.count()).isZero();
    }

    @Test void completingJobCreatesOneInvoiceWithMinimumHours() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        fixtures.fixtureTimesheet(job);
        mockMvc.perform(authorized(patch("/api/job-card/{id}/complete", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        JobCard completed = jobCardRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getTotalWorkedHours()).isEqualByComparingTo("5");
        assertThat(completed.getTotalBillableHours()).isEqualByComparingTo("8");
        assertThat(invoiceRepository.count()).isEqualTo(1);
        assertThat(invoiceRepository.findAll().getFirst().getQty()).isEqualByComparingTo("8");
    }
    @Test void canCancelPendingJob() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(authorized(patch("/api/job-card/{id}/cancel", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
        assertThat(invoiceRepository.count()).isZero();
    }
    @Test void salesCannotCompleteJob() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(asRole(patch("/api/job-card/{id}/complete", job.getId()), "sales@test.local"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(JobCardStatus.class)
    void updatePersistsCraneReassignmentOnlyForPendingJobs(JobCardStatus status) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        job.setStatus(status);
        jobCardRepository.save(job);
        Long originalCraneId = job.getCraneId().getId();
        Crane replacement = replacementCrane(CraneStatus.AVAILABLE);
        Long expectedId = status == JobCardStatus.PENDING ? replacement.getId() : originalCraneId;
        String expectedNumber = status == JobCardStatus.PENDING
                ? replacement.getCraneNumber() : job.getCraneId().getCraneNumber();

        mockMvc.perform(json(asRole(put("/api/job-card/{id}", job.getId()), "operations@test.local"),
                        updateBody(job, replacement.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.craneId").value(expectedId))
                .andExpect(jsonPath("$.crane").value(expectedNumber))
                .andExpect(jsonPath("$.status").value(status.name()))
                .andExpect(jsonPath("$.location").value("Updated location"));
        JobCard persisted = jobCardRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getCraneId().getId()).isEqualTo(expectedId);
        assertThat(persisted.getStatus()).isEqualTo(status);
        mockMvc.perform(authorized(get("/api/job-card/{id}", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.craneId").value(expectedId));
        assertThat(craneRepository.findById(originalCraneId).orElseThrow().getStatus())
                .isEqualTo(CraneStatus.AVAILABLE);
        assertThat(craneRepository.findById(replacement.getId()).orElseThrow().getStatus())
                .isEqualTo(CraneStatus.AVAILABLE);
        assertThat(invoiceRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = CraneStatus.class, names = {"IN_USE", "MAINTENANCE"})
    void updateRejectsUnavailableReplacementWithoutChangingPersistedAssignment(CraneStatus status) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Crane replacement = replacementCrane(status);
        // Business errors currently propagate without a global exception mapper.
        assertThatThrownBy(() -> mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())),
                        updateBody(job, replacement.getId()))))
                .hasRootCauseMessage("Crane is not available");
        assertThat(jobCardRepository.findById(job.getId()).orElseThrow().getCraneId().getId())
                .isEqualTo(job.getCraneId().getId());
    }

    @Test
    void updateRejectsReplacementWithOverlappingScheduledMaintenance() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Crane replacement = replacementCrane(CraneStatus.AVAILABLE);
        Maintenance maintenance = new Maintenance();
        maintenance.setMaintenanceNumber("MT-REASSIGN-001");
        maintenance.setCrane(replacement);
        maintenance.setMaintenanceDate(job.getStartDate());
        maintenance.setMaintenanceStartDate(job.getStartDate());
        maintenance.setMaintenanceEndDate(job.getEndDate());
        maintenance.setMaintenanceType(MaintenanceType.PREVENTIVE);
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        maintenanceRepository.save(maintenance);

        assertThatThrownBy(() -> mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())),
                        updateBody(job, replacement.getId()))))
                .hasRootCauseMessage("Crane is under maintenance during the selected period");
        assertThat(jobCardRepository.findById(job.getId()).orElseThrow().getCraneId().getId())
                .isEqualTo(job.getCraneId().getId());
    }

    private Crane replacementCrane(CraneStatus status) {
        Crane crane = new Crane();
        crane.setCraneNumber("CR-REPLACEMENT");
        crane.setSerialNumber("REPLACEMENT-SERIAL");
        crane.setType("001");
        crane.setStatus(status);
        return craneRepository.save(crane);
    }

    private String updateBody(JobCard job, Long craneId) {
        return """
                {"quotationId":%d,"craneId":%d,"startDate":"%s","endDate":"%s",
                 "location":"Updated location","description":"Reassignment regression"}
                """.formatted(job.getQuotationId().getId(), craneId, job.getStartDate(), job.getEndDate());
    }
}
