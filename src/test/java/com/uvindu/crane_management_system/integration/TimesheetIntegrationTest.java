package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;

class TimesheetIntegrationTest extends AbstractIntegrationTest {
    @Test void createsAndRetrievesTimesheet() throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        Timesheet sheet = fixtures.fixtureTimesheet(job);
        assertThat(sheet.getJobCard().getId()).isEqualTo(job.getId());
        mockMvc.perform(authorized(get("/api/timesheets/{id}", sheet.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workedHours").value(5));
    }
    @Test void filtersTimesheetsByJobCard() throws Exception {
        JobCard job = fixtures.fixtureJobCard(); fixtures.fixtureTimesheet(job);
        mockMvc.perform(authorized(get("/api/timesheets/job-card/{id}", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].workedHours").value(5));
    }
    @Test void updatesTimesheet() throws Exception {
        JobCard job = fixtures.fixtureJobCard(); Timesheet sheet = fixtures.fixtureTimesheet(job);
        String updated = """
                {"dateOfWork":"2026-11-10","workedHours":7,"description":"Updated"}
                """;
        mockMvc.perform(json(authorized(put("/api/timesheets/{id}", sheet.getId())), updated))
                .andExpect(status().isOk());
        assertThat(timesheetRepository.findById(sheet.getId()).orElseThrow().getWorkedHours())
                .isEqualByComparingTo("7");
    }
    @Test void deletesTimesheet() throws Exception {
        JobCard job = fixtures.fixtureJobCard(); Timesheet sheet = fixtures.fixtureTimesheet(job);
        mockMvc.perform(authorized(delete("/api/timesheets/{id}", sheet.getId())))
                .andExpect(status().isNoContent());
        assertThat(timesheetRepository.existsById(sheet.getId())).isFalse();
    }
    @Test void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(json(authorized(post("/api/timesheets")), "{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void salesCannotAccessTimesheets() throws Exception {
        mockMvc.perform(asRole(get("/api/timesheets"), "sales@test.local"))
                .andExpect(status().isForbidden());
    }
}
