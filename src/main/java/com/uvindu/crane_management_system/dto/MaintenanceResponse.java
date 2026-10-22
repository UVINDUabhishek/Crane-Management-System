package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.MaintenanceType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MaintenanceResponse {

    private Long id;

    private String maintenanceNumber;

    private String crane;

    private LocalDate maintenanceDate;

    private LocalDate maintenanceStartDate;

    private LocalDate maintenanceEndDate;

    private MaintenanceType maintenanceType;

    private String description;

    private String maintenancePerson;

    private MaintenanceStatus status;
}
