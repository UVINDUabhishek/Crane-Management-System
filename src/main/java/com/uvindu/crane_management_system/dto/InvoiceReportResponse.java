package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceReportResponse {

    private String invoiceNumber;

    private String customerName;

    private String craneNumber;

    private String jobCardNumber;

    private LocalDate invoiceDate;

    private BigDecimal qty;

    private BigDecimal rate;

    private BigDecimal amount;

    private BigDecimal mobilization;

    private String description;

    private String status;
}
