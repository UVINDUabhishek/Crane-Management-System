package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.CreateMaintenanceRequest;
import com.uvindu.crane_management_system.dto.MaintenanceResponse;
import com.uvindu.crane_management_system.dto.UpdateMaintenanceRequest;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.repository.CraneRepository;
import com.uvindu.crane_management_system.repository.MaintenanceRepository;
import com.uvindu.crane_management_system.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final CraneRepository craneRepository;
    private final UserRepository userRepository;
    private final CraneService craneService;

    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            CraneRepository craneRepository,
            UserRepository userRepository,
            CraneService craneService) {

        this.maintenanceRepository = maintenanceRepository;
        this.craneRepository = craneRepository;
        this.userRepository = userRepository;
        this.craneService = craneService;
    }


    // Create Maintenance
    public MaintenanceResponse createMaintenance(CreateMaintenanceRequest request) {

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> new RuntimeException("Crane not found"));

        User maintenancePerson = null;

        if (request.getMaintenancePersonId() != null) {
            maintenancePerson = userRepository.findById(request.getMaintenancePersonId())
                    .orElseThrow(() -> new RuntimeException("Maintenance person not found"));
        }

        Maintenance maintenance = new Maintenance();

        maintenance.setMaintenanceNumber(generateMaintenanceNumber());
        maintenance.setCrane(crane);
        maintenance.setMaintenanceDate(LocalDate.now());
        maintenance.setMaintenanceStartDate(request.getMaintenanceStartDate());
        maintenance.setMaintenanceEndDate(request.getMaintenanceEndDate());
        maintenance.setMaintenanceType(request.getMaintenanceType());
        maintenance.setDescription(request.getDescription());
        maintenance.setMaintenancePerson(maintenancePerson);
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);

        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);

        return mapToResponse(savedMaintenance);
    }


    // Generate Maintenance Number
    private String generateMaintenanceNumber() {

        return maintenanceRepository.findTopByOrderByIdDesc()
                .map(maintenance -> {

                    String latestNumber = maintenance.getMaintenanceNumber();

                    int number = Integer.parseInt(latestNumber.substring(3));

                    return String.format("MT-%04d", number + 1);
                })
                .orElse("MT-0001");
    }


    // Get All Maintenance Records
    public List<MaintenanceResponse> getAllMaintenance() {

        return maintenanceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // Get Maintenance By ID
    public MaintenanceResponse getMaintenanceById(Long id) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        return mapToResponse(maintenance);
    }


    // Update Maintenance
    public MaintenanceResponse updateMaintenance(
            Long id,
            UpdateMaintenanceRequest request) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> new RuntimeException("Crane not found"));

        maintenance.setCrane(crane);
        maintenance.setMaintenanceStartDate(request.getMaintenanceStartDate());
        maintenance.setMaintenanceEndDate(request.getMaintenanceEndDate());
        maintenance.setMaintenanceType(request.getMaintenanceType());
        maintenance.setDescription(request.getDescription());

        if (request.getMaintenancePersonId() != null) {

            User maintenancePerson = userRepository.findById(
                    request.getMaintenancePersonId()
            ).orElseThrow(() ->
                    new RuntimeException("Maintenance person not found"));

            maintenance.setMaintenancePerson(maintenancePerson);
        } else {
            maintenance.setMaintenancePerson(null);
        }

        Maintenance updatedMaintenance = maintenanceRepository.save(maintenance);

        return mapToResponse(updatedMaintenance);
    }


    // Mapping Method
    private MaintenanceResponse mapToResponse(Maintenance maintenance) {

        MaintenanceResponse response = new MaintenanceResponse();
        response.setCraneId(maintenance.getCrane().getId());

        response.setId(maintenance.getId());
        response.setMaintenanceNumber(maintenance.getMaintenanceNumber());
        response.setCrane(maintenance.getCrane().getCraneNumber());
        response.setMaintenanceDate(maintenance.getMaintenanceDate());
        response.setMaintenanceStartDate(maintenance.getMaintenanceStartDate());
        response.setMaintenanceEndDate(maintenance.getMaintenanceEndDate());
        response.setMaintenanceType(maintenance.getMaintenanceType());
        response.setDescription(maintenance.getDescription());

        if (maintenance.getMaintenancePerson() != null) {
            response.setMaintenancePersonId(maintenance.getMaintenancePerson().getId());
            response.setMaintenancePerson(
                    maintenance.getMaintenancePerson().getUsername()
            );
        }

        response.setStatus(maintenance.getStatus());

        return response;
    }


    // Start Maintenance
    public MaintenanceResponse startMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new RuntimeException("Completed maintenance cannot be started");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw new RuntimeException("Canceled maintenance cannot be started");
        }

        if (maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            throw new RuntimeException("Maintenance is already in progress");
        }

        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);

        craneService.markAsMaintenance(
                maintenance.getCrane().getId()
        );

        return mapToResponse(maintenanceRepository.save(maintenance));
    }


    // Complete Maintenance
    public MaintenanceResponse completeMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new RuntimeException("Maintenance is already completed");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw new RuntimeException("Canceled maintenance cannot be completed");
        }

        maintenance.setStatus(MaintenanceStatus.COMPLETED);

        craneService.markAsAvailable(
                maintenance.getCrane().getId()
        );

        return mapToResponse(maintenanceRepository.save(maintenance));
    }


    // Cancel Maintenance
    public MaintenanceResponse cancelMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new RuntimeException("Completed maintenance cannot be canceled");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw new RuntimeException("Maintenance is already canceled");
        }

        maintenance.setStatus(MaintenanceStatus.CANCELED);

        craneService.markAsAvailable(
                maintenance.getCrane().getId()
        );

        return mapToResponse(maintenanceRepository.save(maintenance));
    }

    // Crane availability support for Job-card
    public boolean isCraneUnderMaintenance(Long craneId, LocalDate jobStartDate, LocalDate jobEndDate) {

        List<MaintenanceStatus> blockingStatuses = List.of(
                MaintenanceStatus.SCHEDULED,
                MaintenanceStatus.IN_PROGRESS
        );

        return maintenanceRepository.existsMaintenanceConflict(
                craneId,
                jobStartDate,
                jobEndDate,
                blockingStatuses
        );
    }

}
