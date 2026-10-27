package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotNull;

public record CraneActivationRequest(
        @NotNull(message = "Activation status is required")
        Boolean active
) {
}
