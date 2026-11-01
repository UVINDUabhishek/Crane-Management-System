package com.uvindu.crane_management_system.dto;

/** Minimal crane identity and booking compatibility data; no pricing or relationships. */
public record CraneLookupResponse(Long id, String label, String type) {
}
