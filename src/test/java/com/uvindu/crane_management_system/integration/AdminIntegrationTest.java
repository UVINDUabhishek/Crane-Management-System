package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class AdminIntegrationTest extends AbstractIntegrationTest {
    private static final String CREATE = """
            {"username":"New Sales","email":"new.sales@test.local",
             "password":"Secure123!","role":"SALES"}
            """;
    @Test void adminCreatesUserWithEncodedPassword() throws Exception {
        mockMvc.perform(json(authorized(post("/api/admin/users")),CREATE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new.sales@test.local"));
        User user = userRepository.findByEmail("new.sales@test.local").orElseThrow();
        assertThat(passwordEncoder.matches("Secure123!",user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.SALES);
    }
    @Test void adminListsAndFindsUsers() throws Exception {
        Long id = userRepository.findByEmail("admin@test.local").orElseThrow().getId();
        mockMvc.perform(authorized(get("/api/admin/users"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());
        mockMvc.perform(authorized(get("/api/admin/users/{id}",id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.local"));
    }
    @Test void updateAndDeactivateNonAdminUser() throws Exception {
        Long id = userRepository.findByEmail("sales@test.local").orElseThrow().getId();
        String change = """
                {"username":"Renamed Sales","email":"sales@test.local","role":"SALES"}
                """;
        mockMvc.perform(json(authorized(put("/api/admin/users/{id}",id)),change))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(id).orElseThrow().getUsername()).isEqualTo("Renamed Sales");
        mockMvc.perform(authorized(delete("/api/admin/users/{id}",id)))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(id).orElseThrow().isActive()).isFalse();
    }
    @Test void nonAdminCannotCreateUsers() throws Exception {
        mockMvc.perform(json(asRole(post("/api/admin/users"),"sales@test.local"),CREATE))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("new.sales@test.local")).isEmpty();
    }
    @Test void anonymousCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
}
