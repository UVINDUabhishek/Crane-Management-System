package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotNull;

public record CustomerActivationRequest(@NotNull Boolean active) { }
