package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.List;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import com.uvindu.crane_management_system.service.LookupService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LookupIntegrationTest extends AbstractIntegrationTest {
    @Autowired private OperatorRepository operatorRepository;
    @Autowired private LookupService lookupService;
    @Autowired private EntityManagerFactory entityManagerFactory;

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

    @Test
    void ordersCraneAndQuotationOptionsByBusinessNumberWithMixedActiveRecords() throws Exception {
        Crane later = saveCrane("CR-020", true);
        Crane earlier = saveCrane("CR-003", true);
        saveCrane("CR-001", false);
        Customer customer = fixtures.customer("ordering-customer");
        Quotation laterQuote = saveQuotation("QT-0020", customer, true);
        Quotation earlierQuote = saveQuotation("QT-0003", customer, true);
        saveQuotation("QT-0001", customer, false);

        assertOrderedIds("cranes", earlier.getId(), later.getId());
        assertOrderedIds("quotations", earlierQuote.getId(), laterQuote.getId());
    }

    @Test
    void ordersCustomerAndPeopleOptionsByNameWithIdTieBreaker() throws Exception {
        Customer later = fixtures.customer("ordering-zulu");
        later.setCompanyName("Zulu"); customerRepository.save(later);
        Customer firstTie = fixtures.customer("ordering-alpha-1");
        firstTie.setCompanyName("Alpha"); customerRepository.save(firstTie);
        Customer secondTie = fixtures.customer("ordering-alpha-2");
        secondTie.setCompanyName("Alpha"); customerRepository.save(secondTie);
        Customer inactive = fixtures.customer("ordering-inactive");
        inactive.setCompanyName("Aardvark"); inactive.setActive(false); customerRepository.save(inactive);
        assertOrderedIds("customers", firstTie.getId(), secondTie.getId(), later.getId());

        User admin = userRepository.findByEmail("admin@test.local").orElseThrow();
        User sales = userRepository.findByEmail("sales@test.local").orElseThrow();
        User operations = userRepository.findByEmail("operations@test.local").orElseThrow();
        User finance = userRepository.findByEmail("finance@test.local").orElseThrow();
        User maintenance = userRepository.findByEmail("maintenance@test.local").orElseThrow();
        admin.setUsername("Same"); sales.setUsername("Same"); operations.setUsername("Same");
        finance.setUsername("Alpha"); maintenance.setUsername("Zulu");
        userRepository.saveAll(List.of(admin, sales, operations, finance, maintenance));
        assertOrderedIds("sales-people", admin.getId(), sales.getId());
        assertOrderedIds("maintenance-people", finance.getId(), admin.getId(), sales.getId(),
                operations.getId(), maintenance.getId());
        // Also verify name ordering for the role-filtered query, independent of insertion order.
        sales.setUsername("Alpha"); userRepository.save(sales);
        assertOrderedIds("sales-people", sales.getId(), admin.getId());
    }

    @Test
    void ordersOperatorsByNameRatherThanInsertionOrder() throws Exception {
        Operator later = saveOperator("Zulu Operator");
        Operator earlier = saveOperator("Alpha Operator");
        assertOrderedIds("operators", earlier.getId(), later.getId());
    }

    @Test
    void lookupQueriesProjectOptionsWithoutHydratingEntitiesOrRelationships() {
        fixtures.fixtureCrane();
        Customer customer = fixtures.customer("projection-customer");
        fixtures.fixtureQuotation(customer);
        saveOperator("Projection Operator");
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(lookupService.getCranes()).hasSize(1);
            assertThat(lookupService.getCustomers()).hasSize(1);
            assertThat(lookupService.getQuotations()).hasSize(1);
            assertThat(lookupService.getOperators()).hasSize(1);
            assertThat(lookupService.getSalesPeople()).hasSize(2);
            assertThat(lookupService.getMaintenancePeople()).hasSize(5);
            assertThat(statistics.getEntityLoadCount()).isZero();
            assertThat(statistics.getEntityFetchCount()).isZero();
            assertThat(statistics.getCollectionFetchCount()).isZero();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
        }
    }

    private Crane saveCrane(String number, boolean active) {
        Crane crane = new Crane();
        crane.setCraneNumber(number);
        crane.setSerialNumber("SERIAL-" + number);
        crane.setType("25T");
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(active);
        return craneRepository.save(crane);
    }

    private Quotation saveQuotation(String number, Customer customer, boolean active) {
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(number);
        quotation.setCustomerId(customer);
        quotation.setActive(active);
        return quotationRepository.save(quotation);
    }

    private Operator saveOperator(String name) {
        Operator operator = new Operator();
        operator.setOperatorName(name);
        return operatorRepository.save(operator);
    }

    private void assertOrderedIds(String resource, Long... ids) throws Exception {
        Integer[] expected = java.util.Arrays.stream(ids).map(Math::toIntExact).toArray(Integer[]::new);
        mockMvc.perform(authorized(get("/api/lookups/" + resource)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", contains(expected)));
    }

    private void assertOption(String resource, Long id, String label) throws Exception {
        mockMvc.perform(authorized(get("/api/lookups/" + resource)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].label").value(label));
    }
}
