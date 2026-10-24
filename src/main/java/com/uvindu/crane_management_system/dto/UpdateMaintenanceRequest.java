package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMaintenanceRequest {

    @NotNull(message = "Crane is required")
    @Positive(message = "Crane Id must be greater than 0")
    private Long craneId;

    @NotNull(message = "Maintenance start date is required")
    private LocalDate maintenanceStartDate;

    private LocalDate maintenanceEndDate;

    @NotNull(message = "Maintenance Type is required")
    private MaintenanceType maintenanceType;

    @NotBlank(message = "Description is required")
    private String description;

    @Positive
    private Long maintenancePersonId;
}
