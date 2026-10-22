package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.JobCardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class JobCardResponse {

    private Long id;

    private String jobCardNumber;

    private String customer;

    private String quotation;

    private String crane;

    private String salesPerson;

    private String operator;

    private LocalDate jobDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalWorkedHours;

    private BigDecimal totalBillableHours;

    private String location;

    private String description;

    private JobCardStatus status;
}
