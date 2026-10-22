package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateQuotationRequest {

    @NotNull(message = "Customer is required")
    @Positive(message = "Customer Id must be greater than 0")
    private Long customerId;

    @NotBlank(message = "Crane type is required")
    private String type;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;

    @NotNull(message = "Minimum hours is required")
    @Positive(message = "Minimum hours must be greater than zero")
    private BigDecimal minHours;

    @NotNull(message = "Minimum rate is required")
    @Positive(message = "Minimum rate must be greater than zero")
    private BigDecimal minRate;

    @Positive(message = "Mobilization must be greater than zero")
    private BigDecimal mobilization;

    @NotBlank(message = "Description is required")
    private String description;

}
