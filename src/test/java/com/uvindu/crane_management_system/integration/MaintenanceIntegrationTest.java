package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

class MaintenanceIntegrationTest extends AbstractIntegrationTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(MaintenanceStatus.class)
    void updateHonorsLifecycle(MaintenanceStatus state) throws Exception {
        Maintenance record = createMaintenance();
        Long craneId = record.getCrane().getId();
        if (state != MaintenanceStatus.SCHEDULED) {
            String action = state == MaintenanceStatus.IN_PROGRESS ? "start"
                    : state == MaintenanceStatus.COMPLETED ? "complete" : "cancel";
            mockMvc.perform(authorized(patch("/api/maintenance/{id}/" + action, record.getId())))
                    .andExpect(status().isOk());
        }
        String before = maintenanceRepository.findById(record.getId()).orElseThrow().getDescription();
        CraneStatus craneBefore = craneRepository.findById(craneId).orElseThrow().getStatus();
        String body = fixtures.maintenanceBody(craneId).replace("Scheduled inspection", "Changed");
        boolean terminal = state == MaintenanceStatus.COMPLETED || state == MaintenanceStatus.CANCELED;
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", record.getId())), body))
                .andExpect(status().is(terminal ? 409 : 200));
        Maintenance persisted = maintenanceRepository.findById(record.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(state);
        assertThat(persisted.getCrane().getId()).isEqualTo(craneId);
        assertThat(craneRepository.findById(craneId).orElseThrow().getStatus()).isEqualTo(craneBefore);
        assertThat(persisted.getDescription()).isEqualTo(terminal ? before : "Changed");
    }

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
    @Test
    void rejectsCreatingMaintenanceForInactiveCrane() throws Exception {

        // Arrange
        Crane crane = fixtures.fixtureCrane();
        crane.setActive(false);
        craneRepository.saveAndFlush(crane);

        long initialCount = maintenanceRepository.count();

        // Act & Assert
        mockMvc.perform(json(
                        authorized(post("/api/maintenance")),
                        fixtures.maintenanceBody(crane.getId())
                ))
                .andExpect(status().isConflict());

        // Verify PostgreSQL remains unchanged
        assertThat(maintenanceRepository.count())
                .isEqualTo(initialCount);

        assertThat(craneRepository.findById(crane.getId())
                .orElseThrow()
                .isActive()).isFalse();
    }

    @Test
    void rejectsUpdatingMaintenanceToInactiveCrane() throws Exception {

        // Arrange: create scheduled maintenance with an active crane
        Maintenance maintenance = createMaintenance();

        Long originalCraneId = maintenance.getCrane().getId();
        Long maintenanceId = maintenance.getId();
        String originalDescription = maintenance.getDescription();

        // Create another crane
        Crane inactiveCrane = new Crane();

        inactiveCrane.setCraneNumber("CR080T999");
        inactiveCrane.setType("080T");
        inactiveCrane.setCapacity(80);
        inactiveCrane.setManufacturer("Test Manufacturer");
        inactiveCrane.setSerialNumber("INACTIVE-TEST-CRANE");
        inactiveCrane.setYear(2022);
        inactiveCrane.setStatus(CraneStatus.AVAILABLE);
        inactiveCrane.setLocation("Colombo");
        inactiveCrane.setActive(false);

        inactiveCrane = craneRepository.saveAndFlush(inactiveCrane);

        // Act: attempt to reassign maintenance to the inactive crane
        mockMvc.perform(json(
                        authorized(put("/api/maintenance/{id}", maintenanceId)),
                        """
                        {
                          "craneId": %d,
                          "maintenanceStartDate": "2026-11-15",
                          "maintenanceEndDate": "2026-11-16",
                          "maintenanceType": "PREVENTIVE",
                          "description": "Should not be saved"
                        }
                        """.formatted(inactiveCrane.getId())
                ))
                .andExpect(status().isConflict());

        // Assert: PostgreSQL record must remain unchanged
        Maintenance persisted = maintenanceRepository.findById(maintenanceId)
                .orElseThrow();

        assertThat(persisted.getCrane().getId())
                .isEqualTo(originalCraneId);

        assertThat(persisted.getDescription())
                .isEqualTo(originalDescription);

        assertThat(persisted.getStatus())
                .isEqualTo(MaintenanceStatus.SCHEDULED);
    }
}
