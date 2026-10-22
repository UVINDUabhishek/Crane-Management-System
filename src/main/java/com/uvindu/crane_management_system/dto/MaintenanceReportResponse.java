package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MaintenanceReportResponse {

    private String maintenanceNumber;

    private String craneNumber;
    private String craneType;

    private LocalDate maintenanceDate;

    private LocalDate startDate;
    private LocalDate endDate;

    private String repairType;

    private String maintenancePerson;

    private String status;
}
