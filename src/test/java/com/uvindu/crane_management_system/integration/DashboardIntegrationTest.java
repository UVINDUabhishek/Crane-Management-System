package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Test void emptyDatabaseDashboardHasZeroCounts() throws Exception {
        mockMvc.perform(authorized(get("/api/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCranes").value(0))
                .andExpect(jsonPath("$.pendingJobs").value(0))
                .andExpect(jsonPath("$.totalInvoices").value(0));
    }

    @Test void craneCountsReflectPersistedCrane() throws Exception {
        fixtures.fixtureCrane();
        mockMvc.perform(authorized(get("/api/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCranes").value(1))
                .andExpect(jsonPath("$.availableCranes").value(1));
    }

    @Test void pendingJobCountReflectsCreatedJob() throws Exception {
        fixtures.fixtureJobCard();
        mockMvc.perform(authorized(get("/api/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingJobs").value(1))
                .andExpect(jsonPath("$.completedJobs").value(0));
    }

    @Test void completedJobAndDraftInvoiceAppearInDashboard() throws Exception {
        fixtures.fixtureInvoice();
        mockMvc.perform(authorized(get("/api/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedJobs").value(1))
                .andExpect(jsonPath("$.totalInvoices").value(1))
                .andExpect(jsonPath("$.draftInvoices").value(1));
    }

    @Test void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void craneWithoutActivityHasAllDaysIdle() throws Exception {

        // Arrange
        fixtures.fixtureCrane();

        // Act & Assert
        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", "2026-10-01")
                                .param("toDate", "2026-10-10"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workedDays").value(0))
                .andExpect(jsonPath("$[0].reservedDays").value(0))
                .andExpect(jsonPath("$[0].maintenanceDays").value(0))
                .andExpect(jsonPath("$[0].idleDays").value(10));
    }

    @Test
    void craneIdleStatisticsReturnEmptyListWhenNoCranesExist()
            throws Exception {

        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", "2026-10-01")
                                .param("toDate", "2026-10-10"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void craneIdleStatisticsRequireAuthentication() throws Exception {

        mockMvc.perform(
                        get("/api/dashboard/crane-idle")
                                .param("fromDate", "2026-10-01")
                                .param("toDate", "2026-10-10")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void craneIdleStatisticsRejectMissingDateParameters()
            throws Exception {

        // Missing fromDate
        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("toDate", "2026-10-10"))
                )
                .andExpect(status().isBadRequest());

        // Missing toDate
        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", "2026-10-01"))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void craneIdleStatisticsRejectInvalidDateFormat()
            throws Exception {

        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", "invalid-date")
                                .param("toDate", "2026-10-10"))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void craneIdleStatisticsRejectReversedDateRange()
            throws Exception {

        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", "2026-10-10")
                                .param("toDate", "2026-10-01"))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void craneIdleStatisticsRejectFutureEndDate()
            throws Exception {

        LocalDate today = LocalDate.now();

        mockMvc.perform(
                        authorized(get("/api/dashboard/crane-idle")
                                .param("fromDate", today.toString())
                                .param("toDate", today.plusDays(1).toString()))
                )
                .andExpect(status().isBadRequest());
    }


}
