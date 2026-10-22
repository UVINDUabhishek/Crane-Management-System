package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CraneUtilizationReportResponse {

    private String craneNumber;

    private String craneType;

    private Integer totalJobs;

    private BigDecimal totalWorkedHours;
}
