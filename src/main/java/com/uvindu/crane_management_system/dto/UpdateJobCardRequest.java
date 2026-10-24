package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

@Data
public class UpdateJobCardRequest {

    @NotNull(message = "Quotation is required")
    @Positive(message = "Quotation Id must be greater than 0")
    private Long quotationId;

    @NotNull(message = "Crane is required")
    @Positive(message = "Crane Id must be greater than 0")
    private Long craneId;

    @Positive(message = "Operator Id must be greater than 0")
    private Long operatorId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @JsonIgnore
    private boolean endDateProvided;

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.endDateProvided = true;
    }


    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Description is required")
    private String description;

}
