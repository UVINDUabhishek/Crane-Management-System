package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class CustomerIntegrationTest extends AbstractIntegrationTest {
    private static final String CREATE = """
            {"companyName":"ABC Logistics","registrationNo":"BATCH-CUS-001",
             "vatNo":"VAT-200","companyAddress":"Colombo","billAddress":"Negombo",
             "contactPerson":"Sam","contactNo":"0771234567","customerEmail":"abc@test.local"}
            """;
    private static final String UPDATE = """
            {"companyName":"ABC Logistics Updated","registrationNo":"BATCH-CUS-001",
             "vatNo":"VAT-200","companyAddress":"Colombo","billAddress":"Negombo",
             "contactPerson":"Sam","contactNo":"0771234567","customerEmail":"abc@test.local"}
            """;
    private Long create() throws Exception {
        mockMvc.perform(json(authorized(post("/api/customers")),CREATE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("ABC Logistics"));
        return customerRepository.findAll().stream().filter(c -> "BATCH-CUS-001".equals(c.getRegistrationNo()))
                .findFirst().orElseThrow().getId();
    }
    @Test void createPersistsCustomer() throws Exception {
        Long id = create();
        assertThat(customerRepository.findById(id).orElseThrow().isActive()).isTrue();
    }
    @Test void readAndUpdateCustomer() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(get("/api/customers/{id}",id))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/customers"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("ABC Logistics"));
        mockMvc.perform(json(authorized(put("/api/customers/{id}",id)),UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("ABC Logistics Updated"));
        assertThat(customerRepository.findById(id).orElseThrow().getCompanyName())
                .isEqualTo("ABC Logistics Updated");
    }
    @Test void deleteSoftDeactivatesCustomer() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(delete("/api/customers/{id}",id))).andExpect(status().isNoContent());
        assertThat(customerRepository.findById(id).orElseThrow().isActive()).isFalse();
    }
    @Test void rejectsInvalidPhoneNumber() throws Exception {
        mockMvc.perform(json(authorized(post("/api/customers")),CREATE.replace("0771234567","123")))
                .andExpect(status().isBadRequest());
        assertThat(customerRepository.count()).isZero();
    }
    @Test void operationsCanReadButCannotCreateCustomers() throws Exception {
        mockMvc.perform(asRole(get("/api/customers"),"operations@test.local"))
                .andExpect(status().isOk());
        mockMvc.perform(json(asRole(post("/api/customers"),"operations@test.local"),CREATE))
                .andExpect(status().isForbidden());
    }
    @Test void financeCannotReadCustomers() throws Exception {
        mockMvc.perform(asRole(get("/api/customers"),"finance@test.local"))
                .andExpect(status().isForbidden());
    }
}
