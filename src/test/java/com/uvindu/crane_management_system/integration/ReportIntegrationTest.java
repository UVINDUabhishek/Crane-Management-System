package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;

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
}
