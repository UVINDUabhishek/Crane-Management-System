package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobCardReportResponse {

    private String jobCardNumber;

    private String customerName;

    private String craneNumber;

    private String salesPerson;

    private String operatorName;

    private LocalDate jobDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private String location;

    private BigDecimal totalWorkedHours;

    private BigDecimal totalBillableHours;

    private String status;
}
