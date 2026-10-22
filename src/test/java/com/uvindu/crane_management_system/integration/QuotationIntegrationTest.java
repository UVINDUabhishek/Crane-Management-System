package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

class QuotationIntegrationTest extends AbstractIntegrationTest {
    private String createJson(Long customerId) {
        return """
            {"customerId":%d,"type":"012T","validFrom":"2026-10-01",
             "validTo":"2026-12-31","minHours":8,"minRate":12500,
             "mobilization":15000,"description":"Eight hour crane hire"}
            """.formatted(customerId);
    }
    private static final String UPDATE = """
            {"type":"012T","validFrom":"2026-10-01",
             "validTo":"2026-12-31","minHours":10,"minRate":13500,
             "mobilization":16000,"description":"Updated crane hire"}
            """;
    private Long create() throws Exception {
        Customer c = fixtures.customer("BATCH-Q-CUSTOMER");
        mockMvc.perform(json(authorized(post("/api/quotations")),createJson(c.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quotationNumber").value("QT-0001"));
        return quotationRepository.findAll().getFirst().getId();
    }
    @Test void createPersistsQuotationWithCustomerLink() throws Exception {
        Long id = create();
        Quotation q = quotationRepository.findById(id).orElseThrow();
        assertThat(q.getCustomerId().getCompanyName()).isEqualTo("Integration Test Customer");
        assertThat(q.getMinHours()).isEqualByComparingTo("8");
    }
    @Test void listAndUpdateQuotation() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(get("/api/quotations/{id}",id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationNumber").value("QT-0001"));
        mockMvc.perform(authorized(get("/api/quotations"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quotationNumber").value("QT-0001"));
        mockMvc.perform(json(authorized(put("/api/quotations/{id}",id)),UPDATE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Updated crane hire"));
        assertThat(quotationRepository.findById(id).orElseThrow().getMinHours())
                .isEqualByComparingTo("10");
    }
    @Test void deleteSoftDeactivatesQuotation() throws Exception {
        Long id = create();
        mockMvc.perform(authorized(delete("/api/quotations/{id}",id))).andExpect(status().isNoContent());
        assertThat(quotationRepository.findById(id).orElseThrow().isActive()).isFalse();
    }
    @Test void rejectsMissingCustomerId() throws Exception {
        mockMvc.perform(json(authorized(post("/api/quotations")),createJson(1L).replace("\"customerId\":1,","")))
                .andExpect(status().isBadRequest());
        assertThat(quotationRepository.count()).isZero();
    }
    @Test void salesCanReadButOperationsCannot() throws Exception {
        mockMvc.perform(asRole(get("/api/quotations"),"sales@test.local")).andExpect(status().isOk());
        mockMvc.perform(asRole(get("/api/quotations"),"operations@test.local"))
                .andExpect(status().isForbidden());
    }
}
