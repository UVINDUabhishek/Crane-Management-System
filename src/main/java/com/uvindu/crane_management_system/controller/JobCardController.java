package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateJobCardRequest;
import com.uvindu.crane_management_system.dto.JobCardResponse;
import com.uvindu.crane_management_system.dto.UpdateJobCardRequest;
import com.uvindu.crane_management_system.service.JobCardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-card")
public class JobCardController {

    private final JobCardService jobCardService;

    public JobCardController(JobCardService jobCardService) {
        this.jobCardService = jobCardService;
    }

    // Create a Job-card
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<JobCardResponse> createJobCard(@Valid @RequestBody CreateJobCardRequest request) {

        JobCardResponse response = jobCardService.createJobCard(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get all Job-cards
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<List<JobCardResponse>> getAllJobCards() {

        return ResponseEntity.ok(jobCardService.getAllJobCards());
    }

    // Get Job-card by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<JobCardResponse> getJobCardById(@PathVariable Long id) {

        return ResponseEntity.ok(jobCardService.getJobCardById(id));
    }



    // Update Job-card
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<JobCardResponse> updateJobCard(@PathVariable Long id, @Valid @RequestBody UpdateJobCardRequest request) {

        return ResponseEntity.ok(jobCardService.updateJobCard(id, request));
    }


    // Complete Job-card
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS')")
    public ResponseEntity<JobCardResponse> completeJobCard(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                jobCardService.completeJobCard(id)
        );
    }


    // Cancel Job-card
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<JobCardResponse> cancelJobCard(@PathVariable Long id) {

        return ResponseEntity.ok(jobCardService.cancelJobCard(id));
    }

}
