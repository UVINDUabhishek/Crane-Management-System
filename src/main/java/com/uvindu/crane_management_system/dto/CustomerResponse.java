package com.uvindu.crane_management_system.dto;

import lombok.Data;

@Data
public class CustomerResponse {

    private Long id;

    private String companyName;

    private String registrationNo;

    private String vatNo;

    private String companyAddress;

    private String billAddress;

    private String contactPerson;

    private String contactNo;

    private String customerEmail;

}