package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
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
}
