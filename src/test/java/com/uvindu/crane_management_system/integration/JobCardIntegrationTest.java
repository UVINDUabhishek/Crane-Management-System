package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
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
}
