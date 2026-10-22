package com.uvindu.crane_management_system.dto;

import lombok.Data;

@Data
public class CraneMaintenanceSummaryResponse {

    private Long craneId;

    private String craneNumber;

    private String craneType;

    private Long totalMaintenanceRecords;

    private Long totalMaintenanceDays;

}
