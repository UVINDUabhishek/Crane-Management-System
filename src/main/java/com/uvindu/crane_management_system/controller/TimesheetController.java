package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateTimesheetRequest;
import com.uvindu.crane_management_system.dto.TimesheetResponse;
import com.uvindu.crane_management_system.dto.UpdateTimesheetRequest;
import com.uvindu.crane_management_system.service.TimesheetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS')")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    // Create timesheet
    @PostMapping
    public ResponseEntity<TimesheetResponse> createTimesheet(
            @Valid @RequestBody CreateTimesheetRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(timesheetService.createTimesheet(request));
    }

    // Get all timesheets
    @GetMapping
    public ResponseEntity<List<TimesheetResponse>> getAllTimesheets() {

        return ResponseEntity.ok(
                timesheetService.getAllTimesheets()
        );
    }

    // Get timesheet by ID
    @GetMapping("/{id}")
    public ResponseEntity<TimesheetResponse> getTimesheetById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                timesheetService.getTimesheetById(id)
        );
    }

    // Get all timesheets by Job-cards
    @GetMapping("/job-card/{jobCardId}")
    public ResponseEntity<List<TimesheetResponse>> getTimesheetsByJobCard(
            @PathVariable Long jobCardId) {

        return ResponseEntity.ok(
                timesheetService.getTimesheetsByJobCard(jobCardId)
        );
    }

    // Update timesheet
    @PutMapping("/{id}")
    public ResponseEntity<TimesheetResponse> updateTimesheet(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimesheetRequest request) {

        return ResponseEntity.ok(
                timesheetService.updateTimesheet(id, request)
        );
    }

    // Delete timesheet
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimesheet(@PathVariable Long id) {

        timesheetService.deleteTimesheet(id);

        return ResponseEntity.noContent().build();
    }


}
