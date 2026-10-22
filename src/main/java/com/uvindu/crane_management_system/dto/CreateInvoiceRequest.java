package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateInvoiceRequest {

    @NotNull(message = "Quantity is required")
    @Positive(message = "qty must be greater than 0")
    private BigDecimal qty;

    @NotNull(message = "Rate is required")
    @Positive(message = "rate must be greater than 0")
    private BigDecimal rate;

    private BigDecimal mobilization;

    @NotBlank(message = "Description is required")
    private String description;
}
