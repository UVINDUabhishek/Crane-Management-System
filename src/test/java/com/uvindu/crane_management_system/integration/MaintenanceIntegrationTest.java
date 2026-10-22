package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

class MaintenanceIntegrationTest extends AbstractIntegrationTest {
    private Maintenance createMaintenance() throws Exception {
        Crane crane = fixtures.fixtureCrane();
        mockMvc.perform(json(authorized(post("/api/maintenance")), fixtures.maintenanceBody(crane.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SCHEDULED"));
        return maintenanceRepository.findAll().getFirst();
    }
    @Test void persistsScheduledMaintenance() throws Exception {
        Maintenance maintenance = createMaintenance();
        assertThat(maintenance.getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED);
        mockMvc.perform(authorized(get("/api/maintenance/{id}", maintenance.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SCHEDULED"));
    }
    @Test void startsAndCompletesMaintenanceWithCraneStatusChanges() throws Exception {
        Maintenance maintenance = createMaintenance(); Long craneId = maintenance.getCrane().getId();
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", maintenance.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(craneRepository.findById(craneId).orElseThrow().getStatus())
                .isEqualTo(CraneStatus.MAINTENANCE);
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/complete", maintenance.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(craneRepository.findById(craneId).orElseThrow().getStatus())
                .isEqualTo(CraneStatus.AVAILABLE);
    }
    @Test void cancelsScheduledMaintenance() throws Exception {
        Maintenance maintenance = createMaintenance();
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/cancel", maintenance.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
    }
    @Test void rejectsIncompleteMaintenanceRequest() throws Exception {
        mockMvc.perform(json(authorized(post("/api/maintenance")), "{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void operationsCannotStartMaintenance() throws Exception {
        Maintenance maintenance = createMaintenance();
        mockMvc.perform(asRole(patch("/api/maintenance/{id}/start", maintenance.getId()), "operations@test.local"))
                .andExpect(status().isForbidden());
    }
    @Test void financeCannotReadMaintenance() throws Exception {
        mockMvc.perform(asRole(get("/api/maintenance"), "finance@test.local"))
                .andExpect(status().isForbidden());
    }
}
