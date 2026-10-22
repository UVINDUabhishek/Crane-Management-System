package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateCustomerRequest;
import com.uvindu.crane_management_system.dto.CustomerResponse;
import com.uvindu.crane_management_system.dto.UpdateCustomerRequest;
import com.uvindu.crane_management_system.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }


    // Create a customer
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get all customers
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {

        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // Get customer by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {

        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // Update customer
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest request) {

        return ResponseEntity.ok(customerService.updateCustomer(id,request));
    }

    // Delete customer
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.noContent().build();
    }

}
