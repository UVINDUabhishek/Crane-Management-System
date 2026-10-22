package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOperatorRequest {

    @NotBlank(message = "Operator name is required")
    private String operatorName;
}
