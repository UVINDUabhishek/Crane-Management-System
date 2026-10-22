package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateCraneRequest;
import com.uvindu.crane_management_system.dto.UpdateCraneRequest;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;

public class CraneTestDataBuilder {

    public static Crane createCrane(Long id, String craneNumber, CraneStatus status) {
        Crane crane = new Crane();
        crane.setId(id);
        crane.setCraneNumber(craneNumber);
        crane.setType("MOBILE");
        crane.setCapacity(50.0);
        crane.setManufacturer("Liebherr");
        crane.setSerialNumber("SN-" + id);
        crane.setYear(2020);
        crane.setStatus(status);
        crane.setLocation("Colombo");
        crane.setActive(true);
        return crane;
    }

    public static CreateCraneRequest createCraneRequest() {
        CreateCraneRequest request = new CreateCraneRequest();
        request.setType("MOBILE");
        request.setCapacity(50.0);
        request.setManufacturer("Liebherr");
        request.setSerialNumber("SN-NEW");
        request.setYear(2020);
        request.setLocation("Colombo");
        return request;
    }

    public static UpdateCraneRequest createUpdateCraneRequest() {
        UpdateCraneRequest request = new UpdateCraneRequest();
        request.setType("TOWER");
        request.setCapacity(100.0);
        request.setManufacturer("Potain");
        request.setSerialNumber("SN-UPDATED");
        request.setYear(2021);
        request.setLocation("Kandy");
        return request;
    }
}
