package com.uvindu.crane_management_system.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
public class LoginRequest {

    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;

}
