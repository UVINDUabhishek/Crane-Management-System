package com.uvindu.crane_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimesheetResponse {

    private String timesheetNumber;

    private String jobCardNumber;

    private String customerName;

    private LocalDate dateOfWork;

    private BigDecimal workedHours;

    private String description;
}
