package com.uvindu.crane_management_system.dto;

import java.time.LocalDate;

/** Minimal booking compatibility data; no commercial pricing. */
public record QuotationLookupResponse(Long id, String label, Long customerId,
                                      String type, LocalDate validFrom, LocalDate validTo) {
}
