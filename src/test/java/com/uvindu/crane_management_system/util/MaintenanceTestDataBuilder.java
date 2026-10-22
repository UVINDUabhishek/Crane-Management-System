package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateMaintenanceRequest;
import com.uvindu.crane_management_system.dto.UpdateMaintenanceRequest;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.MaintenanceType;
import com.uvindu.crane_management_system.model.User;

import java.time.LocalDate;

public class MaintenanceTestDataBuilder {

    public static Maintenance createMaintenance(Long id, String maintenanceNumber, MaintenanceStatus status) {
        Maintenance maintenance = new Maintenance();
        maintenance.setId(id);
        maintenance.setMaintenanceNumber(maintenanceNumber);
        maintenance.setMaintenanceDate(LocalDate.now());
        maintenance.setMaintenanceStartDate(LocalDate.now());
        maintenance.setMaintenanceEndDate(LocalDate.now().plusDays(3));
        maintenance.setMaintenanceType(MaintenanceType.PREVENTIVE);
        maintenance.setDescription("Test maintenance");
        maintenance.setStatus(status);
        return maintenance;
    }

    public static CreateMaintenanceRequest createMaintenanceRequest(Long craneId) {
        CreateMaintenanceRequest request = new CreateMaintenanceRequest();
        request.setCraneId(craneId);
        request.setMaintenanceStartDate(LocalDate.now());
        request.setMaintenanceEndDate(LocalDate.now().plusDays(3));
        request.setMaintenanceType(MaintenanceType.PREVENTIVE);
        request.setDescription("Test maintenance");
        request.setMaintenancePersonId(1L);
        return request;
    }

    public static UpdateMaintenanceRequest createUpdateMaintenanceRequest(Long craneId) {
        UpdateMaintenanceRequest request = new UpdateMaintenanceRequest();
        request.setCraneId(craneId);
        request.setMaintenanceStartDate(LocalDate.now().plusDays(1));
        request.setMaintenanceEndDate(LocalDate.now().plusDays(4));
        request.setMaintenanceType(MaintenanceType.CORRECTIVE);
        request.setDescription("Updated maintenance");
        request.setMaintenancePersonId(2L);
        return request;
    }

    public static Crane createCrane(Long id) {
        Crane crane = new Crane();
        crane.setId(id);
        crane.setCraneNumber("CRMOBILE001");
        return crane;
    }

    public static User createMaintenancePerson(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("maintuser");
        return user;
    }
}
