package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueSummaryResponse {

    private Long totalInvoices;

    private BigDecimal totalAmount;

    private BigDecimal totalMobilization;

    private BigDecimal totalRevenue;
}
