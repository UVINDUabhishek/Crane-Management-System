package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateUserRequest {

    @NotBlank(message = "User name is required")
    private String username;

    @NotBlank(message = "User email is required")
    @Email(message = "Correct email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "User role is required")
    private UserRole role;

}
