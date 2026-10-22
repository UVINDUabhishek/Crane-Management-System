package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyRevenueReportResponse {

    private Integer year;

    private Integer month;

    private String monthName;

    private BigDecimal totalAmount;

    private BigDecimal totalMobilization;

    private BigDecimal totalRevenue;

    private Long totalInvoices;
}
