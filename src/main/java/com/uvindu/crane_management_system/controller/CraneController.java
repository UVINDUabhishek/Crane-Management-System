package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CraneActivationRequest;
import com.uvindu.crane_management_system.dto.CraneResponse;
import com.uvindu.crane_management_system.dto.CreateCraneRequest;
import com.uvindu.crane_management_system.dto.UpdateCraneRequest;
import com.uvindu.crane_management_system.service.CraneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cranes")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
public class CraneController {

    private final CraneService craneService;

    public CraneController(CraneService craneService) {
        this.craneService = craneService;
    }

    // Create a crane
    @PostMapping
    public ResponseEntity<CraneResponse> createCrane(@Valid @RequestBody CreateCraneRequest request) {

        CraneResponse response = craneService.createCrane(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    // Get all cranes
    @GetMapping
    public ResponseEntity<List<CraneResponse>> getAllCranes() {

        return ResponseEntity.ok(craneService.getAllCranes());
    }


    // Get crane by ID
    @GetMapping("/{id}")
    public ResponseEntity<CraneResponse> getCraneById(@PathVariable Long id) {

        return ResponseEntity.ok(craneService.getCraneById(id));
    }

    // Update crane
    @PutMapping("/{id}")
    public ResponseEntity<CraneResponse> updateCrane(@PathVariable Long id, @Valid @RequestBody UpdateCraneRequest request) {

        return ResponseEntity.ok(craneService.updateCrane(id, request));
    }


    // Delete crane
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCrane(@PathVariable Long id) {

        craneService.deleteCrane(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activation")
    public ResponseEntity<CraneResponse> updateCraneActivation(
            @PathVariable Long id,
            @Valid @RequestBody CraneActivationRequest request
    ) {

        CraneResponse response = craneService.updateActivation(
                id,
                request.active()
        );

        return ResponseEntity.ok(response);
    }

}
