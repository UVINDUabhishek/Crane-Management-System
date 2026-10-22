package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateCraneRequest {

    @NotBlank(message = "Crane type is required")
    private String type;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than 0")
    private double capacity;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotNull(message = "Year is required")
    @Positive(message = "Year must be greater than 0")
    private int year;

    @NotBlank(message = "Location is required")
    private String location;
}
