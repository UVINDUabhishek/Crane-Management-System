package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.dto.QuotationLookupResponse;
import com.uvindu.crane_management_system.service.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {
    private final LookupService lookupService;

    @GetMapping("/cranes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE')")
    public List<LookupResponse> cranes() {
        return lookupService.getCranes();
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public List<LookupResponse> customers() {
        return lookupService.getCustomers();
    }

    @GetMapping("/quotations")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public List<QuotationLookupResponse> quotations() {
        return lookupService.getQuotations();
    }

    @GetMapping("/operators")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public List<LookupResponse> operators() {
        return lookupService.getOperators();
    }

    @GetMapping("/sales-people")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public List<LookupResponse> salesPeople() {
        return lookupService.getSalesPeople();
    }

    @GetMapping("/maintenance-people")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public List<LookupResponse> maintenancePeople() {
        return lookupService.getMaintenancePeople();
    }
}
