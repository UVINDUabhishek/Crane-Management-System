package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LookupIntegrationTest extends AbstractIntegrationTest {
    @Autowired private OperatorRepository operatorRepository;

    @BeforeEach
    void addMaintenanceUser() {
        User user = new User();
        user.setUsername("Maintenance");
        user.setEmail("maintenance@test.local");
        user.setPassword(passwordEncoder.encode("TestPass123!"));
        user.setRole(UserRole.MAINTENANCE);
        userRepository.save(user);
        // Operators are not covered by the shared fixture's cascading truncate.
        operatorRepository.deleteAll();
    }

    @Test
    void checksEveryRoleAgainstEveryLookupAndRejectsAnonymousRequests() throws Exception {
        Map<String, Set<UserRole>> allowed = Map.of(
                "cranes", Set.of(UserRole.values()),
                "customers", Set.of(UserRole.ADMIN, UserRole.SALES, UserRole.OPERATIONS, UserRole.FINANCE),
                "quotations", Set.of(UserRole.ADMIN, UserRole.SALES, UserRole.OPERATIONS),
                "operators", Set.of(UserRole.ADMIN, UserRole.SALES, UserRole.OPERATIONS),
                "sales-people", Set.of(UserRole.ADMIN, UserRole.SALES, UserRole.OPERATIONS, UserRole.FINANCE),
                "maintenance-people", Set.of(UserRole.ADMIN, UserRole.OPERATIONS, UserRole.MAINTENANCE));
        for (var entry : allowed.entrySet()) {
            String path = "/api/lookups/" + entry.getKey();
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (User user : userRepository.findAll()) {
                mockMvc.perform(asRole(get(path), user.getEmail()))
                        .andExpect(status().is(allowed.get(entry.getKey()).contains(user.getRole()) ? 200 : 403));
            }
        }
    }

    @Test
    void returnsMinimalStableOptionsAndExcludesInactiveRecords() throws Exception {
        Customer customer = fixtures.customer("lookup-customer");
        Crane crane = fixtures.fixtureCrane();
        Quotation quotation = fixtures.fixtureQuotation(customer);
        Operator operator = new Operator();
        operator.setOperatorName("Test Operator");
        operatorRepository.save(operator);
        assertOption("cranes", crane.getId(), crane.getCraneNumber());
        assertOption("customers", customer.getId(), customer.getCompanyName());
        assertOption("operators", operator.getId(), operator.getOperatorName());
        mockMvc.perform(asRole(get("/api/lookups/quotations"), "operations@test.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].length()").value(3))
                .andExpect(jsonPath("$[0].id").value(quotation.getId()))
                .andExpect(jsonPath("$[0].label").value(quotation.getQuotationNumber()))
                .andExpect(jsonPath("$[0].customerId").value(customer.getId()))
                .andExpect(jsonPath("$[0].minRate").doesNotExist());
        customer.setActive(false); customerRepository.save(customer);
        crane.setActive(false); craneRepository.save(crane);
        quotation.setActive(false); quotationRepository.save(quotation);
        for (String resource : new String[]{"customers", "cranes", "quotations"}) {
            mockMvc.perform(authorized(get("/api/lookups/" + resource)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void peopleLookupsExposeOnlyIdsAndNamesWithDocumentedEligibility() throws Exception {
        User sales = userRepository.findByEmail("sales@test.local").orElseThrow();
        User admin = userRepository.findByEmail("admin@test.local").orElseThrow();
        mockMvc.perform(asRole(get("/api/lookups/sales-people"), "finance@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + sales.getId() + ")].label").value("SALES"))
                .andExpect(jsonPath("$[?(@.id == " + admin.getId() + ")].label").value("ADMIN"))
                .andExpect(jsonPath("$[0].length()").value(2));
        mockMvc.perform(asRole(get("/api/lookups/maintenance-people"), "maintenance@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[*].password").isEmpty())
                .andExpect(jsonPath("$[*].email").isEmpty());
        sales.setActive(false); userRepository.save(sales);
        mockMvc.perform(authorized(get("/api/lookups/sales-people")))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(authorized(get("/api/lookups/maintenance-people")))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void lookupAccessDoesNotGrantAdminOrBusinessPermissions() throws Exception {
        for (User user : userRepository.findAll()) {
            if (user.getRole() == UserRole.ADMIN) continue;
            for (String path : new String[]{"/api/admin/users", "/api/admin/operators"}) {
                mockMvc.perform(asRole(get(path), user.getEmail())).andExpect(status().isForbidden());
                mockMvc.perform(json(asRole(post(path), user.getEmail()), "{}"))
                        .andExpect(status().isForbidden());
                mockMvc.perform(asRole(delete(path + "/1"), user.getEmail()))
                        .andExpect(status().isForbidden());
            }
        }
        for (String email : new String[]{"operations@test.local", "finance@test.local", "maintenance@test.local"}) {
            mockMvc.perform(asRole(get("/api/cranes"), email)).andExpect(status().isForbidden());
        }
        mockMvc.perform(asRole(get("/api/quotations"), "operations@test.local"))
                .andExpect(status().isForbidden());
        mockMvc.perform(asRole(get("/api/customers"), "finance@test.local"))
                .andExpect(status().isForbidden());
        mockMvc.perform(asRole(get("/api/invoices"), "sales@test.local"))
                .andExpect(status().isForbidden());
        mockMvc.perform(asRole(get("/api/timesheets"), "sales@test.local"))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(authorized(post("/api/lookups/operators")), "{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private void assertOption(String resource, Long id, String label) throws Exception {
        mockMvc.perform(authorized(get("/api/lookups/" + resource)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].label").value(label));
    }
}
