package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvoiceResponse {

    private Long customerId;
    private Long craneId;
    private Long quotationId;
    private Long jobCardId;

    private Long id;

    private String invoiceNumber;

    private String customer;

    private String crane;

    private String quotation;

    private String jobCard;

    private LocalDate invoiceDate;

    private BigDecimal qty;

    private BigDecimal rate;

    private BigDecimal amount;

    private BigDecimal mobilization;

    private String description;

    private InvoiceStatus status;

    private Integer version;
}
