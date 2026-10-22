package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.UserRole;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserResponse {

    private Long id;

    private String username;
    private String email;
    private UserRole role;
}
