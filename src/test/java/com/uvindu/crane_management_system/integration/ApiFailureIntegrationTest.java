package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiFailureIntegrationTest extends AbstractIntegrationTest {
    @MockitoBean CustomerService customerService;

    @Test
    void unexpectedFailuresRemain500AndNeverExposeInternalDetails() throws Exception {
        when(customerService.getAllCustomers()).thenThrow(new IllegalStateException("SQL credentials password=private stacktrace"));
        String body = mockMvc.perform(authorized(get("/api/customers")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("SQL", "private", "password", "stacktrace", "Exception");
    }

    @Test
    void lockFailuresReturnRetryable409WithoutDatabaseDetails() throws Exception {
        when(customerService.getAllCustomers()).thenThrow(new PessimisticLockingFailureException("PostgreSQL lock SQL detail"));
        mockMvc.perform(authorized(get("/api/customers"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }
}
