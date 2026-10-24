package com.uvindu.crane_management_system.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;

}
