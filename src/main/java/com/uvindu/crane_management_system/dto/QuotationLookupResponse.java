package com.uvindu.crane_management_system.dto;

/** Customer identity supports filtering quotations without exposing pricing. */
public record QuotationLookupResponse(Long id, String label, Long customerId) {
}
