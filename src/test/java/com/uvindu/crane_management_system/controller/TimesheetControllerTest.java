package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateTimesheetRequest;
import com.uvindu.crane_management_system.dto.TimesheetResponse;
import com.uvindu.crane_management_system.dto.UpdateTimesheetRequest;
import com.uvindu.crane_management_system.service.TimesheetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetControllerTest {

    @Mock
    private TimesheetService timesheetService;

    @InjectMocks
    private TimesheetController timesheetController;

    // ==================== CREATE ====================

    @Test
    void shouldCreateTimesheet() {

        CreateTimesheetRequest request = new CreateTimesheetRequest();
        TimesheetResponse timesheetResponse = new TimesheetResponse();

        when(timesheetService.createTimesheet(request))
                .thenReturn(timesheetResponse);

        ResponseEntity<TimesheetResponse> response =
                timesheetController.createTimesheet(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(timesheetResponse);

        verify(timesheetService).createTimesheet(request);
    }

    // ==================== GET ALL ====================

    @Test
    void shouldGetAllTimesheets() {

        TimesheetResponse timesheet1 = new TimesheetResponse();
        TimesheetResponse timesheet2 = new TimesheetResponse();

        List<TimesheetResponse> timesheetList =
                List.of(timesheet1, timesheet2);

        when(timesheetService.getAllTimesheets())
                .thenReturn(timesheetList);

        ResponseEntity<List<TimesheetResponse>> response =
                timesheetController.getAllTimesheets();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(timesheetList);
        assertThat(response.getBody()).hasSize(2);

        verify(timesheetService).getAllTimesheets();
    }

    // ==================== GET BY ID ====================

    @Test
    void shouldGetTimesheetById() {

        Long timesheetId = 1L;
        TimesheetResponse timesheetResponse = new TimesheetResponse();

        when(timesheetService.getTimesheetById(timesheetId))
                .thenReturn(timesheetResponse);

        ResponseEntity<TimesheetResponse> response =
                timesheetController.getTimesheetById(timesheetId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(timesheetResponse);

        verify(timesheetService).getTimesheetById(timesheetId);
    }

    // ==================== GET BY JOB CARD ====================

    @Test
    void shouldGetTimesheetsByJobCard() {

        Long jobCardId = 1L;

        TimesheetResponse timesheet1 = new TimesheetResponse();
        TimesheetResponse timesheet2 = new TimesheetResponse();

        List<TimesheetResponse> timesheetList =
                List.of(timesheet1, timesheet2);

        when(timesheetService.getTimesheetsByJobCard(jobCardId))
                .thenReturn(timesheetList);

        ResponseEntity<List<TimesheetResponse>> response =
                timesheetController.getTimesheetsByJobCard(jobCardId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(timesheetList);
        assertThat(response.getBody()).hasSize(2);

        verify(timesheetService).getTimesheetsByJobCard(jobCardId);
    }

    // ==================== UPDATE ====================

    @Test
    void shouldUpdateTimesheet() {

        Long timesheetId = 1L;
        UpdateTimesheetRequest request = new UpdateTimesheetRequest();
        TimesheetResponse updatedTimesheet = new TimesheetResponse();

        when(timesheetService.updateTimesheet(timesheetId, request))
                .thenReturn(updatedTimesheet);

        ResponseEntity<TimesheetResponse> response =
                timesheetController.updateTimesheet(timesheetId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedTimesheet);

        verify(timesheetService).updateTimesheet(timesheetId, request);
    }

    // ==================== DELETE ====================

    @Test
    void shouldDeleteTimesheet() {

        Long timesheetId = 1L;

        ResponseEntity<Void> response =
                timesheetController.deleteTimesheet(timesheetId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(timesheetService).deleteTimesheet(timesheetId);
    }
}