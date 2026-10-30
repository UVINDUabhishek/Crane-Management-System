package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class CraneIntegrationTest extends AbstractIntegrationTest {
    private static final String CREATE = """
            {"type":"012T","capacity":50,"manufacturer":"Liebherr",
             "serialNumber":"BATCH-CR-001","year":2022,"location":"Colombo"}
            """;
    private static final String UPDATE = """
            {"type":"012T","capacity":60,"manufacturer":"Tadano",
             "serialNumber":"BATCH-CR-001","year":2023,"location":"Negombo"}
            """;
    private Long create() throws Exception {
        mockMvc.perform(json(authorized(post("/api/cranes")),CREATE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.serialNumber").value("BATCH-CR-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        return craneRepository.findAll().stream().filter(c -> "BATCH-CR-001".equals(c.getSerialNumber()))
                .findFirst().orElseThrow().getId();
    }
    @Test void createPersistsCrane() throws Exception {
        Long id = create();
        assertThat(craneRepository.findById(id)).isPresent();
        assertThat(craneRepository.findById(id).orElseThrow().isActive()).isTrue();
    }
    @Test void getListAndUpdate() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(get("/api/cranes/{id}",id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("BATCH-CR-001"));
        mockMvc.perform(authorized(get("/api/cranes"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serialNumber").value("BATCH-CR-001"));
        mockMvc.perform(json(authorized(put("/api/cranes/{id}",id)),UPDATE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(60.0));
        assertThat(craneRepository.findById(id).orElseThrow().getLocation()).isEqualTo("Negombo");
    }
    @Test void deleteSoftDeactivatesCrane() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(delete("/api/cranes/{id}",id))).andExpect(status().isNoContent());
        assertThat(craneRepository.findById(id).orElseThrow().isActive()).isFalse();
    }
    @Test void validationRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(json(authorized(post("/api/cranes")),"{}"))
                .andExpect(status().isBadRequest());
        assertThat(craneRepository.count()).isZero();
    }
    @Test void operationsCannotAccessCraneController() throws Exception {
        mockMvc.perform(asRole(get("/api/cranes"),"operations@test.local"))
                .andExpect(status().isForbidden());
    }
    @Test void salesCanReadCranes() throws Exception {
        mockMvc.perform(asRole(get("/api/cranes"),"sales@test.local"))
                .andExpect(status().isOk());
    }
    @Test
    void shouldDeactivateAndReactivateCrane() throws Exception {

        // Arrange
        Long craneId = create();

        // Act & Assert — Deactivate
        mockMvc.perform(json(
                        authorized(patch("/api/cranes/{id}/activation", craneId)),
                        """
                        {"active":false}
                        """
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        assertThat(craneRepository.findById(craneId)
                .orElseThrow()
                .isActive()).isFalse();

        // Act & Assert — Reactivate
        mockMvc.perform(json(
                        authorized(patch("/api/cranes/{id}/activation", craneId)),
                        """
                        {"active":true}
                        """
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        assertThat(craneRepository.findById(craneId)
                .orElseThrow()
                .isActive()).isTrue();
    }

    @Test
    void activationValidationAuthorizationAndLookup() throws Exception {
        Long id = create();
        createMaintenanceUser();
        for (String role : new String[]{"admin", "sales", "operations", "finance", "maintenance"}) {
            mockMvc.perform(json(asRole(patch("/api/cranes/{id}/activation", id), role + "@test.local"),
                            "{\"active\":false}"))
                    .andExpect(status().is(role.equals("admin") || role.equals("sales") ? 200 : 403));
        }
        mockMvc.perform(json(patch("/api/cranes/{id}/activation", id), "{\"active\":true}"))
                .andExpect(status().isUnauthorized());
        for (String body : new String[]{"{}", "{\"active\":null}"}) {
            mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(json(authorized(patch("/api/cranes/999999/activation")), "{\"active\":true}"))
                .andExpect(status().isNotFound());
        assertThat(craneRepository.findActiveLookupOptions()).isEmpty();
        mockMvc.perform(authorized(get("/api/cranes/{id}", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(authorized(get("/api/cranes")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].active").value(false));
        mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), "{\"active\":true}"))
                .andExpect(status().isOk());
        assertThat(craneRepository.findActiveLookupOptions()).hasSize(1);
    }

    @Test
    void unfinishedWorkBlocksPatchAndDeleteButHistorySurvives() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Long id = job.getCraneId().getId();
        mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), "{\"active\":false}"))
                .andExpect(status().isConflict());
        mockMvc.perform(authorized(delete("/api/cranes/{id}", id))).andExpect(status().isConflict());
        assertThat(craneRepository.findById(id).orElseThrow().isActive()).isTrue();
        mockMvc.perform(authorized(patch("/api/job-card/{id}/cancel", job.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(delete("/api/cranes/{id}", id))).andExpect(status().isNoContent());
        assertThat(jobCardRepository.findById(job.getId()).orElseThrow().getCraneId().getId()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unfinishedMaintenanceBlocksDeactivation(boolean start) throws Exception {
        Long id = create();
        mockMvc.perform(json(authorized(post("/api/maintenance")), fixtures.maintenanceBody(id)))
                .andExpect(status().isCreated());
        Long maintenanceId = maintenanceRepository.findAll().getFirst().getId();
        if (start) {
            mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", maintenanceId)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), "{\"active\":false}"))
                .andExpect(status().isConflict());
        mockMvc.perform(authorized(delete("/api/cranes/{id}", id))).andExpect(status().isConflict());
        assertThat(craneRepository.findById(id).orElseThrow().isActive()).isTrue();
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/cancel", maintenanceId)))
                .andExpect(status().isOk());
        mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), "{\"active\":false}"))
                .andExpect(status().isOk());
        assertThat(maintenanceRepository.findById(maintenanceId).orElseThrow().getCrane().getId()).isEqualTo(id);
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void crudAuthorizationMatchesControllerPolicy(UserRole role) throws Exception {
        Long id = create();
        createMaintenanceUser();
        String email = role.name().toLowerCase(Locale.ROOT) + "@test.local";
        boolean allowed = role == UserRole.ADMIN || role == UserRole.SALES;
        mockMvc.perform(asRole(get("/api/cranes"), email)).andExpect(status().is(allowed ? 200 : 403));
        mockMvc.perform(asRole(get("/api/cranes/{id}", id), email)).andExpect(status().is(allowed ? 200 : 403));
        mockMvc.perform(json(asRole(post("/api/cranes"), email), CREATE.replace("BATCH-CR-001", "NEW-SERIAL")))
                .andExpect(status().is(allowed ? 201 : 403));
        mockMvc.perform(json(asRole(put("/api/cranes/{id}", id), email), UPDATE))
                .andExpect(status().is(allowed ? 200 : 403));
        mockMvc.perform(asRole(delete("/api/cranes/{id}", id), email))
                .andExpect(status().is(allowed ? 204 : 403));
    }

    @Test
    void duplicateSerialUpdateFailsWithoutChangingStoredFields() throws Exception {
        Long id = create();
        mockMvc.perform(json(authorized(post("/api/cranes")), CREATE.replace("BATCH-CR-001", "OTHER-SERIAL")))
                .andExpect(status().isCreated());
        Crane before = craneRepository.findById(id).orElseThrow();
        mockMvc.perform(json(authorized(put("/api/cranes/{id}", id)), UPDATE.replace("BATCH-CR-001", "OTHER-SERIAL")))
                .andExpect(status().isConflict());
        Crane after = craneRepository.findById(id).orElseThrow();
        assertThat(after.getSerialNumber()).isEqualTo(before.getSerialNumber());
        assertThat(after.getLocation()).isEqualTo(before.getLocation());
        // Successful ordinary edits also leave backend-controlled properties alone.
        mockMvc.perform(json(authorized(patch("/api/cranes/{id}/activation", id)), "{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(json(authorized(put("/api/cranes/{id}", id)), UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.craneNumber").value(before.getCraneNumber()))
                .andExpect(jsonPath("$.status").value(before.getStatus().name()))
                .andExpect(jsonPath("$.active").value(false));
    }

    private void createMaintenanceUser() {
        User user = new User();
        user.setUsername("Maint");
        user.setEmail("maintenance@test.local");
        user.setPassword(passwordEncoder.encode("TestPass123!"));
        user.setRole(UserRole.MAINTENANCE);
        user.setActive(true);
        userRepository.save(user);
    }
}
