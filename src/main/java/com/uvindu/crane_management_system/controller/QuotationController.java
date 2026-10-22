package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateQuotationRequest;
import com.uvindu.crane_management_system.dto.QuotationResponse;
import com.uvindu.crane_management_system.dto.UpdateQuotationRequest;
import com.uvindu.crane_management_system.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    // Create a quotation
    @PostMapping
    public ResponseEntity<QuotationResponse> createQuotation(@Valid @RequestBody CreateQuotationRequest request) {

        QuotationResponse response = quotationService.createQuotation(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    // Get all quotations
    @GetMapping
    public ResponseEntity<List<QuotationResponse>> getAllQuotations() {

        return ResponseEntity.ok(quotationService.getAllQuotations());

    }

    // Get Quotation by ID
    @GetMapping("/{id}")
    public ResponseEntity<QuotationResponse> getQuotationById(@PathVariable Long id) {

        return ResponseEntity.ok(quotationService.getQuotationById(id));

    }

    // Update Quotation
    @PutMapping("/{id}")
    public ResponseEntity<QuotationResponse> updateQuotation(@PathVariable Long id, @Valid @RequestBody UpdateQuotationRequest request) {

        return ResponseEntity.ok(quotationService.updateQuotation(id, request));

    }

    // Delete Quotation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuotation(@PathVariable Long id) {

        quotationService.deleteQuotation(id);

        return ResponseEntity.noContent().build();

    }

}
