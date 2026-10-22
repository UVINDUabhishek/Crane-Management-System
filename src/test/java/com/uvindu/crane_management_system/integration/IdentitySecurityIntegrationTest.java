package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import org.junit.jupiter.api.Test;
import com.uvindu.crane_management_system.controller.AuthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IdentitySecurityIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AuthController authController;

    @Test
    void identityMethodRequiresAuthenticationEvenOutsideHttpFilterChain() {
        var previousContext = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
        try {
            var suppliedPrincipal = new UsernamePasswordAuthenticationToken("admin@test.local", null);
            assertThatThrownBy(() -> authController.me(suppliedPrincipal))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    @Test
    void identityReturnsOnlySafeFieldsForEveryRole() throws Exception {
        User maintenance = new User();
        maintenance.setUsername("Maintenance");
        maintenance.setEmail("maintenance@test.local");
        maintenance.setRole(UserRole.MAINTENANCE);
        maintenance.setPassword(passwordEncoder.encode("TestPass123!"));
        userRepository.save(maintenance);

        for (User user : userRepository.findAll()) {
            mockMvc.perform(asRole(get("/api/auth/me"), user.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.role").value(user.getRole().name()))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }

    @Test
    void identityRejectsAnonymousAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivationRejectsPreviouslyIssuedTokenAndPasswordLogin() throws Exception {
        String login = """
                {"email":"sales@test.local","password":"TestPass123!"}
                """;
        String issuedToken = mockMvc.perform(json(post("/api/auth/login"), login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + issuedToken))
                .andExpect(status().isOk());
        User sales = userRepository.findByEmail("sales@test.local").orElseThrow();
        mockMvc.perform(authorized(delete("/api/admin/users/{id}", sales.getId())))
                .andExpect(status().isNoContent());
        for (String path : new String[]{"/api/auth/me", "/api/customers", "/api/lookups/operators"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + issuedToken))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(json(post("/api/auth/login"), login)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreationReturnsSafeContractAndPreservesEncoding() throws Exception {
        String body = """
                {"username":"New Sales","email":"phase0@test.local","password":"TestPass123!","role":"SALES"}
                """;
        String response = mockMvc.perform(json(authorized(post("/api/admin/users")), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("New Sales"))
                .andExpect(jsonPath("$.email").value("phase0@test.local"))
                .andExpect(jsonPath("$.role").value("SALES"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        User saved = userRepository.findByEmail("phase0@test.local").orElseThrow();
        assertThat(passwordEncoder.matches("TestPass123!", saved.getPassword())).isTrue();
        assertThat(response).doesNotContain(saved.getPassword(), "TestPass123!");
        mockMvc.perform(authorized(get("/api/admin/users/{id}", saved.getId())))
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.password").doesNotExist());
        mockMvc.perform(authorized(get("/api/admin/users")))
                .andExpect(jsonPath("$[?(@.email == 'phase0@test.local')].id").value(org.hamcrest.Matchers.hasItem(saved.getId().intValue())));
        mockMvc.perform(json(authorized(put("/api/admin/users/{id}", saved.getId())), body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    void identityUsesAuthenticatedPrincipalAndCurrentRole() throws Exception {
        String issuedToken = token("sales@test.local");
        User sales = userRepository.findByEmail("sales@test.local").orElseThrow();
        sales.setRole(UserRole.FINANCE);
        userRepository.save(sales);
        mockMvc.perform(get("/api/auth/me?email=admin@test.local").header("Authorization", issuedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sales.getId()))
                .andExpect(jsonPath("$.role").value("FINANCE"));
        mockMvc.perform(get("/api/quotations").header("Authorization", issuedToken))
                .andExpect(status().isForbidden());
    }
}
