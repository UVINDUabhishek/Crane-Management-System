package com.uvindu.crane_management_system.integration;

import com.jayway.jsonpath.JsonPath;
import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FrontendContractIntegrationTest extends AbstractIntegrationTest {
    @Test
    void responseIdsSupportQuotationJobAndTimesheetWorkflow() throws Exception {
        Customer customer = fixtures.customer("contract-customer");
        Crane crane = fixtures.fixtureCrane();
        String quoteBody = """
                {"customerId":%d,"type":"001","validFrom":"2026-11-01","validTo":"2026-11-30",
                 "minHours":8,"minRate":1000,"mobilization":500,"description":"Contract quote"}
                """.formatted(customer.getId());
        String quoteResponse = mockMvc.perform(json(asRole(post("/api/quotations"), "sales@test.local"), quoteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.customerName").value(customer.getCompanyName()))
                .andExpect(jsonPath("$.quotationNumber").value("QT-0001"))
                .andReturn().getResponse().getContentAsString();
        long quotationId = id(quoteResponse);
        mockMvc.perform(authorized(get("/api/quotations")))
                .andExpect(jsonPath("$[0].id").value(quotationId));
        mockMvc.perform(authorized(get("/api/quotations/{id}", quotationId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(quotationId));
        mockMvc.perform(json(authorized(put("/api/quotations/{id}", quotationId)), quoteBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(quotationId))
                .andExpect(jsonPath("$.customerId").value(customer.getId()));

        String operatorResponse = mockMvc.perform(json(authorized(post("/api/admin/operators")),
                        "{\"operatorName\":\"Contract Operator\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long operatorId = id(operatorResponse);
        long salespersonId = userRepository.findByEmail("sales@test.local").orElseThrow().getId();
        String jobBody = """
                {"customerId":%d,"quotationId":%d,"craneId":%d,"operatorId":%d,
                 "startDate":"2026-11-10","endDate":"2026-11-12","location":"Colombo","description":"Contract job"}
                """.formatted(customer.getId(), quotationId, crane.getId(), operatorId);
        String jobResponse = mockMvc.perform(json(asRole(post("/api/job-card"), "sales@test.local"), jobBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.quotationId").value(quotationId))
                .andExpect(jsonPath("$.craneId").value(crane.getId()))
                .andExpect(jsonPath("$.salesPersonId").value(salespersonId))
                .andExpect(jsonPath("$.operatorId").value(operatorId))
                .andExpect(jsonPath("$.operator").value("Contract Operator"))
                .andReturn().getResponse().getContentAsString();
        long jobId = id(jobResponse);
        mockMvc.perform(authorized(get("/api/job-card/{id}", jobId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quotationId").value(quotationId));
        mockMvc.perform(authorized(get("/api/job-card")))
                .andExpect(jsonPath("$[0].operatorId").value(operatorId));
        mockMvc.perform(json(asRole(put("/api/job-card/{id}", jobId), "operations@test.local"), jobBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salesPersonId").value(salespersonId));

        String sheetBody = """
                {"jobCardId":%d,"dateOfWork":"2020-01-01","workedHours":5,"description":"Contract timesheet"}
                """.formatted(jobId);
        String sheetResponse = mockMvc.perform(json(asRole(post("/api/timesheets"), "operations@test.local"), sheetBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.jobCardId").value(jobId))
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.timesheetNumber").value("TS-0001"))
                .andReturn().getResponse().getContentAsString();
        long sheetId = id(sheetResponse);
        mockMvc.perform(authorized(get("/api/timesheets/{id}", sheetId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sheetId));
        mockMvc.perform(authorized(get("/api/timesheets")))
                .andExpect(jsonPath("$[0].id").value(sheetId));
        mockMvc.perform(authorized(get("/api/timesheets/job-card/{id}", jobId)))
                .andExpect(jsonPath("$[0].id").value(sheetId));
        mockMvc.perform(json(authorized(put("/api/timesheets/{id}", sheetId)), sheetBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sheetId))
                .andExpect(jsonPath("$.jobCardId").value(jobId));
        mockMvc.perform(authorized(delete("/api/timesheets/{id}", sheetId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void maintenanceResponsePreservesOptionalAssigneeAndRelatedIds() throws Exception {
        Crane crane = fixtures.fixtureCrane();
        User assignee = userRepository.findByEmail("operations@test.local").orElseThrow();
        String body = fixtures.maintenanceBody(crane.getId());
        String response = mockMvc.perform(json(asRole(post("/api/maintenance"), "operations@test.local"), body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.craneId").value(crane.getId()))
                .andExpect(jsonPath("$.crane").value(crane.getCraneNumber()))
                .andExpect(jsonPath("$.maintenancePersonId").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        long maintenanceId = id(response);
        String assigned = body.replace("}", ",\"maintenancePersonId\":" + assignee.getId() + "}");
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenanceId)), assigned))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maintenancePersonId").value(assignee.getId()))
                .andExpect(jsonPath("$.maintenancePerson").value(assignee.getUsername()));
        mockMvc.perform(authorized(get("/api/maintenance/{id}", maintenanceId)))
                .andExpect(jsonPath("$.craneId").value(crane.getId()))
                .andExpect(jsonPath("$.maintenancePersonId").value(assignee.getId()));
        mockMvc.perform(authorized(get("/api/maintenance")))
                .andExpect(jsonPath("$[0].maintenancePersonId").value(assignee.getId()));
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenanceId)), body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maintenancePersonId").value(nullValue()));
    }

    @Test
    void invoiceRelatedIdsPreserveExistingDisplayFieldsAndNullJobOperator() throws Exception {
        Invoice invoice = fixtures.fixtureInvoice();
        mockMvc.perform(authorized(get("/api/job-card/{id}", invoice.getJobCard().getId())))
                .andExpect(jsonPath("$.operatorId").value(nullValue()));
        for (String path : new String[]{"/api/invoices/" + invoice.getId(), "/api/invoices"}) {
            String prefix = path.equals("/api/invoices") ? "$[0]" : "$";
            mockMvc.perform(asRole(get(path), "finance@test.local"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(prefix + ".customerId").value(invoice.getCustomer().getId()))
                    .andExpect(jsonPath(prefix + ".craneId").value(invoice.getCrane().getId()))
                    .andExpect(jsonPath(prefix + ".quotationId").value(invoice.getQuotation().getId()))
                    .andExpect(jsonPath(prefix + ".jobCardId").value(invoice.getJobCard().getId()))
                    .andExpect(jsonPath(prefix + ".crane").value("BATCH2-CR-SERIAL"))
                    .andExpect(jsonPath(prefix + ".amount").value(8500))
                    .andExpect(jsonPath(prefix + ".status").value("DRAFT"));
        }
        mockMvc.perform(json(authorized(put("/api/invoices/{id}", invoice.getId())),
                        "{\"qty\":8,\"rate\":1000,\"mobilization\":500,\"description\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jobCardId").value(invoice.getJobCard().getId()));
    }

    private long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
