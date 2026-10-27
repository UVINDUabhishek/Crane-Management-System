package com.uvindu.crane_management_system.dto;

import com.uvindu.crane_management_system.model.CraneStatus;
import lombok.Data;

@Data
public class CraneResponse {

    private Long id;

    private String craneNumber;

    private String type;

    private double capacity;

    private String manufacturer;

    private String serialNumber;

    private int year;

    private CraneStatus status;

    private boolean active;

    private String location;

}
