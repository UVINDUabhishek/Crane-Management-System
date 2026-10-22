package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTimesheetRequest {

    @NotNull(message = "Date of work is required")
    private LocalDate dateOfWork;

    @DecimalMin(value = "0.0", inclusive = false,
    message = "Worked hours must be greater than 0")
    private BigDecimal workedHours;

    @NotBlank(message = "Description is required")
    private String description;
}
