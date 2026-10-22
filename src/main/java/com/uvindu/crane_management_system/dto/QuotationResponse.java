package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class QuotationResponse {

    private Long id;
    private Long customerId;

    private String quotationNumber;

    private String customerName;

    private String type;

    private LocalDate validFrom;

    private LocalDate validTo;

    private BigDecimal minHours;

    private BigDecimal minRate;

    private BigDecimal mobilization;

    private String description;

}
