package com.uvindu.crane_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Registration number is required")
    private String registrationNo;

    @NotBlank(message = "VAT number is required")
    private String vatNo;

    @NotBlank(message = "Company address is required")
    private String companyAddress;

    @NotBlank(message = "Bill address is required")
    private String billAddress;

    @NotBlank(message = "Contact Person is required")
    private String contactPerson;

    @NotNull(message = "Contact number is required")
    @Pattern(
            regexp = "^(0\\d{9})$",
            message = "Contact number must be a valid 10-digit Sri Lankan phone number"
    )
    private String contactNo;

    @Email(message = "Invalid email address")
    private String customerEmail;

}