package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {

        userRepository.deleteAll();

        User admin = new User();

        admin.setUsername("Admin");
        admin.setEmail("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode("12345"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "admin@gmail.com",
                                    "password": "12345"
                                }
                                """)
        )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLoginWithInvalidPassword() throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content("""
                            {
                                "email": "admin@gmail.com",
                                "password": "wrong-password"
                            }
                            """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLoginWithUnknownEmail() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "unknown@gmail.com",
                                    "password": "12345"
                                }
                                """)
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAccessProtectedEndpointWithValidJwt() throws Exception {

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content("""
                            {
                                "email": "admin@gmail.com",
                                "password": "12345"
                            }
                            """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String token = loginResult.getResponse().getContentAsString();

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }


    @Test
    void shouldRejectAccessToProtectedEndpointWithoutJwt() throws Exception {

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonAdminAccessToAdminEndpoint() throws Exception {

        User salesUser = new User();

        salesUser.setUsername("Sales");
        salesUser.setEmail("sales@gmail.com");

        salesUser.setPassword(passwordEncoder.encode("12345"));
        salesUser.setRole(UserRole.SALES);
        salesUser.setActive(true);

        userRepository.save(salesUser);

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                "email":"sales@gmail.com",
                                "password":"12345"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andReturn();

        String token = loginResult.getResponse().getContentAsString();

        mockMvc.perform(
                get("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAccessWithInvalidJwt() throws Exception {

        mockMvc.perform(
                get("/api/admin/users")
                        .header("Authorization", "Bearer invalid.jwt.token")
        )
                .andExpect(status().isUnauthorized());
    }


}