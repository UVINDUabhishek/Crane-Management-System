package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateMaintenanceRequest;
import com.uvindu.crane_management_system.dto.MaintenanceResponse;
import com.uvindu.crane_management_system.dto.UpdateMaintenanceRequest;
import com.uvindu.crane_management_system.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    // Create Maintenance
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody CreateMaintenanceRequest request) {

        MaintenanceResponse response =
                maintenanceService.createMaintenance(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get All Maintenance
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<List<MaintenanceResponse>> getAllMaintenance() {

        return ResponseEntity.ok(
                maintenanceService.getAllMaintenance()
        );
    }

    // Get Maintenance By ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                maintenanceService.getMaintenanceById(id)
        );
    }

    // Update Maintenance
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceRequest request) {

        return ResponseEntity.ok(
                maintenanceService.updateMaintenance(id, request)
        );
    }

    // Start Maintenance
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> startMaintenance(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                maintenanceService.startMaintenance(id)
        );
    }

    // Complete Maintenance
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> completeMaintenance(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                maintenanceService.completeMaintenance(id)
        );
    }

    // Cancel Maintenance
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE')")
    public ResponseEntity<MaintenanceResponse> cancelMaintenance(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                maintenanceService.cancelMaintenance(id)
        );
    }

}
