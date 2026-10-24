package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkflowIntegrationTest extends AbstractIntegrationTest {

    @Test void completingJobAfterTimesheetCreatesExactlyOneDraftInvoice() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        assertThat(invoiceRepository.count()).isZero();
        fixtures.fixtureTimesheet(job);

        mockMvc.perform(asRole(patch("/api/job-card/{id}/complete", job.getId()), "operations@test.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(jobCardRepository.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobCardStatus.COMPLETED);
        assertThat(invoiceRepository.count()).isEqualTo(1);
        Invoice invoice = invoiceRepository.findAll().getFirst();
        assertThat(invoice.getJobCard().getId()).isEqualTo(job.getId());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getVersion()).isEqualTo(1);
        assertThat(invoice.getAmount()).isPositive();
    }

    @Test void completingJobWithoutTimesheetsDoesNotCreateInvoice() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        // The current controller has no exception mapper for this business exception.
        mockMvc.perform(
                authorized(patch("/api/job-card/{id}/complete", job.getId())))
                .andExpect(status().isConflict());
        assertThat(invoiceRepository.count()).isZero();
        assertThat(jobCardRepository.findById(job.getId()).orElseThrow().getStatus())
                .isEqualTo(JobCardStatus.PENDING);
    }

    @Test void cancelingPendingJobDoesNotGenerateInvoice() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        mockMvc.perform(asRole(patch("/api/job-card/{id}/cancel", job.getId()), "sales@test.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void completedWorkflowIsVisibleToFinanceInInvoiceReports() throws Exception {

        // Arrange
        Invoice invoice = fixtures.fixtureInvoice();

        Long jobCardId = invoice.getJobCard().getId();

        String expectedJobCardNumber = jobCardRepository
                .findById(jobCardId)
                .orElseThrow()
                .getJobCardNumber();

        // Act & Assert
        mockMvc.perform(
                        asRole(
                                get("/api/reports/invoices"),
                                "finance@test.local"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobCardNumber")
                        .value(expectedJobCardNumber))
                .andExpect(jsonPath("$[0].status")
                        .value("DRAFT"));
    }

}
