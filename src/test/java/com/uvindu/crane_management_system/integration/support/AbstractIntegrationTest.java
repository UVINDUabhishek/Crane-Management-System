package com.uvindu.crane_management_system.integration.support;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.repository.*;
import com.uvindu.crane_management_system.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CustomerRepository customerRepository;
    @Autowired protected CraneRepository craneRepository;
    @Autowired protected QuotationRepository quotationRepository;
    @Autowired protected JobCardRepository jobCardRepository;
    @Autowired protected TimesheetRepository timesheetRepository;
    @Autowired protected MaintenanceRepository maintenanceRepository;
    @Autowired protected InvoiceRepository invoiceRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;
    @Autowired protected JdbcTemplate jdbcTemplate;

    protected IntegrationTestDataFactory fixtures;

    @BeforeEach
    void resetIntegrationDatabase() {
        // Only execute against the dedicated PostgreSQL Testcontainers database.
        // CASCADE also clears dependent tables. Do not run tests in parallel against one DB.
        jdbcTemplate.execute("TRUNCATE TABLE users, cranes, customers, quotations RESTART IDENTITY CASCADE");
        createTestUser("admin@test.local", UserRole.ADMIN);
        createTestUser("sales@test.local", UserRole.SALES);
        createTestUser("operations@test.local", UserRole.OPERATIONS);
        createTestUser("finance@test.local", UserRole.FINANCE);
        fixtures = new IntegrationTestDataFactory(customerRepository, craneRepository,
                quotationRepository, jobCardRepository, timesheetRepository,
                invoiceRepository, mockMvc, jwtService);
    }

    private void createTestUser(String email, UserRole role) {
        User user = new User();
        user.setUsername(role.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("TestPass123!"));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
    }

    protected String token(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    protected MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return asRole(request, "admin@test.local");
    }

    protected MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request, String email) {
        return request.header("Authorization", token(email));
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType("application/json").content(body);
    }
}
