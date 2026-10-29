package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class FrontendWorkflowIntegrationTest extends AbstractIntegrationTest {
    @Test
    void hireTransactionAndMaintenanceUseRealHttpContracts() throws Exception {
        mockMvc.perform(json(authorized(post("/api/customers")), """
                {"companyName":"Workflow Customer","registrationNo":"WF-1","vatNo":"VAT-1",
                "companyAddress":"Colombo","billAddress":"Colombo","contactPerson":"Alex",
                "contactNo":"0771234567","customerEmail":"workflow@example.com"}
                """)).andExpect(status().isCreated());
        long customer = customerRepository.findAll().getFirst().getId();
        mockMvc.perform(json(authorized(post("/api/cranes")), """
                {"type":"080T","capacity":80,"manufacturer":"Test","serialNumber":"WF-CR-1",
                "year":2024,"location":"Colombo"}
                """)).andExpect(status().isCreated());
        long crane = craneRepository.findAll().getFirst().getId();
        mockMvc.perform(json(authorized(post("/api/quotations")), """
                {"customerId":%d,"type":"080T","validFrom":"2026-10-01","validTo":"2026-12-31",
                "minHours":8,"minRate":1000,"mobilization":500,"description":"Workflow quote"}
                """.formatted(customer))).andExpect(status().isCreated());
        long quotation = quotationRepository.findAll().getFirst().getId();
        mockMvc.perform(json(authorized(post("/api/job-card")), """
                {"customerId":%d,"quotationId":%d,"craneId":%d,"startDate":"2026-11-10",
                "endDate":"2026-11-10","location":"Colombo","description":"Workflow hire"}
                """.formatted(customer, quotation, crane))).andExpect(status().isCreated());
        long job = jobCardRepository.findAll().getFirst().getId();
        mockMvc.perform(json(authorized(post("/api/timesheets")), """
                {"jobCardId":%d,"dateOfWork":"2026-11-10","workedHours":5,"description":"Worked"}
                """.formatted(job))).andExpect(status().isCreated());
        mockMvc.perform(authorized(patch("/api/job-card/{id}/complete", job)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(invoiceRepository.count()).isEqualTo(1);
        long invoice = invoiceRepository.findAll().getFirst().getId();
        mockMvc.perform(authorized(patch("/api/invoices/{id}/complete", invoice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(authorized(get("/api/reports/revenue-summary"))).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/reports/invoices")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].invoiceNumber").exists());
        mockMvc.perform(json(authorized(post("/api/maintenance")), fixtures.maintenanceBody(crane)))
                .andExpect(status().isCreated());
        long maintenance = maintenanceRepository.findAll().getFirst().getId();
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", maintenance))).andExpect(status().isOk());
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/complete", maintenance)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
