package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

class InvoiceIntegrationTest extends AbstractIntegrationTest {
    @Test void automaticallyGeneratedInvoiceCanBeRetrieved() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        mockMvc.perform(authorized(get("/api/invoices/{id}", invoice.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
    }
    @Test void listsGeneratedInvoices() throws Exception {
        fixtures.fixtureInvoice();
        mockMvc.perform(authorized(get("/api/invoices")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("DRAFT"));
    }
    @Test void financeCanCompleteDraftInvoice() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(asRole(patch("/api/invoices/{id}/complete", invoice.getId()), "finance@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }
    @Test void financeCanCancelDraftInvoice() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(asRole(patch("/api/invoices/{id}/cancel", invoice.getId()), "finance@test.local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
    }
    @Test void salesCannotReadInvoices() throws Exception {
        mockMvc.perform(asRole(get("/api/invoices"), "sales@test.local"))
                .andExpect(status().isForbidden());
    }
    @Test void rejectsInvalidInvoiceUpdate() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(json(authorized(put("/api/invoices/{id}", invoice.getId())), "{}"))
                .andExpect(status().isBadRequest());
    }
}
