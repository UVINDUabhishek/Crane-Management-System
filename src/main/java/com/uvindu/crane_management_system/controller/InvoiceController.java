package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateInvoiceRequest;
import com.uvindu.crane_management_system.dto.InvoiceResponse;
import com.uvindu.crane_management_system.dto.UpdateInvoiceRequest;
import com.uvindu.crane_management_system.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // Get all invoices
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {

        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    // Get invoice by ID
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {

        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    // Update invoice
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(@PathVariable Long id,@Valid @RequestBody UpdateInvoiceRequest request) {

        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    // Complete invoice
    @PatchMapping("/{id}/complete")
    public ResponseEntity<InvoiceResponse> completeInvoice(@PathVariable Long id) {

        return ResponseEntity.ok(invoiceService.completeInvoice(id));
    }

    // Cancel invoice
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable Long id) {

        return ResponseEntity.ok(invoiceService.cancelInvoice(id));
    }


    // Revise invoice
    @PatchMapping("/{id}/revise")
    public ResponseEntity<InvoiceResponse> reviseInvoice(@PathVariable Long id, @Valid @RequestBody CreateInvoiceRequest request) {

        return ResponseEntity.ok(invoiceService.reviseInvoice(id, request));
    }

}
