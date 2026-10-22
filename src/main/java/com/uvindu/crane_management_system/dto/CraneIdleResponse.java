package com.uvindu.crane_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class CraneIdleResponse {

    private Long craneId;
    private String craneNumber;

    private long workedDays;
    private long reservedDays;
    private long maintenanceDays;
    private long idleDays;

}
