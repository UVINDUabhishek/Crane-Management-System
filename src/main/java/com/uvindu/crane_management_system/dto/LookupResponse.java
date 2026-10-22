package com.uvindu.crane_management_system.dto;

/** Minimal display data for a selectable record; never an entity serialization. */
public record LookupResponse(Long id, String label) {
}
