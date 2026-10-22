package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTimesheetRequest {

    @NotNull(message = "Job card is required")
    private Long jobCardId;

    @NotNull(message = "Date of work is required")
    private LocalDate dateOfWork;

    @NotNull(message = "Worked hours are required")
    @DecimalMin(value = "0.0", inclusive = false,
    message = "Worked hours must be greater than 0")
    private BigDecimal workedHours;

    private String description;
}
