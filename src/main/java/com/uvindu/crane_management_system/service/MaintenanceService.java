package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
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
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final CraneRepository craneRepository;
    private final UserRepository userRepository;
    private final CraneService craneService;
    private final SchedulingService schedulingService;

    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            CraneRepository craneRepository,
            UserRepository userRepository,
            CraneService craneService, SchedulingService schedulingService) {
        this.schedulingService = schedulingService;

        this.maintenanceRepository = maintenanceRepository;
        this.craneRepository = craneRepository;
        this.userRepository = userRepository;
        this.craneService = craneService;
    }


    // Create Maintenance
    @Transactional
    public MaintenanceResponse createMaintenance(CreateMaintenanceRequest request) {

        SchedulingService.validateDates(request.getMaintenanceStartDate(), request.getMaintenanceEndDate());
        schedulingService.lockCranes(request.getCraneId());
        schedulingService.rejectBookingConflict(request.getCraneId(), request.getMaintenanceStartDate(),
                request.getMaintenanceEndDate(), null);

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> BusinessException.notFound("Crane not found"));

        if (!crane.isActive()) {
            throw BusinessException.conflict(
                    "Cannot schedule maintenance for an inactive crane"
            );
        }

        User maintenancePerson = null;

        if (request.getMaintenancePersonId() != null) {
            maintenancePerson = userRepository.findById(request.getMaintenancePersonId())
                    .orElseThrow(() -> BusinessException.notFound("Maintenance person not found"));
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
                .orElseThrow(() -> BusinessException.notFound("Maintenance not found"));

        return mapToResponse(maintenance);
    }


    // Update Maintenance
    @Transactional
    public MaintenanceResponse updateMaintenance(
            Long id,
            UpdateMaintenanceRequest request) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED
                || maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw BusinessException.conflict("Completed or canceled maintenance cannot be updated");
        }

        SchedulingService.validateDates(request.getMaintenanceStartDate(), request.getMaintenanceEndDate());
        Long previousCraneId = maintenance.getCrane().getId();
        schedulingService.lockCranes(previousCraneId, request.getCraneId());

        if (maintenance.getStatus() == MaintenanceStatus.SCHEDULED
                || maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            schedulingService.rejectBookingConflict(request.getCraneId(), request.getMaintenanceStartDate(),
                    request.getMaintenanceEndDate(), null);
        }

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> BusinessException.notFound("Crane not found"));

        if (!crane.isActive()) {
            throw BusinessException.conflict(
                    "Cannot update maintenance for an inactive crane"
            );
        }

        maintenance.setCrane(crane);
        maintenance.setMaintenanceStartDate(request.getMaintenanceStartDate());
        maintenance.setMaintenanceEndDate(request.getMaintenanceEndDate());
        maintenance.setMaintenanceType(request.getMaintenanceType());
        maintenance.setDescription(request.getDescription());

        if (request.getMaintenancePersonId() != null) {

            User maintenancePerson = userRepository.findById(
                    request.getMaintenancePersonId()
            ).orElseThrow(() ->
                    BusinessException.notFound("Maintenance person not found"));

            maintenance.setMaintenancePerson(maintenancePerson);
        } else {
            maintenance.setMaintenancePerson(null);
        }

        Maintenance updatedMaintenance = maintenanceRepository.save(maintenance);
        if (maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            craneService.markAsMaintenance(crane.getId());
            if (!previousCraneId.equals(crane.getId())) {
                craneService.markAsAvailable(previousCraneId);
            }
        }

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
    @Transactional
    public MaintenanceResponse startMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw BusinessException.conflict("Completed maintenance cannot be started");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw BusinessException.conflict("Canceled maintenance cannot be started");
        }

        if (maintenance.getStatus() == MaintenanceStatus.IN_PROGRESS) {
            throw BusinessException.conflict("Maintenance is already in progress");
        }

        schedulingService.lockCranes(maintenance.getCrane().getId());
        schedulingService.rejectBookingConflict(maintenance.getCrane().getId(),
                maintenance.getMaintenanceStartDate(), maintenance.getMaintenanceEndDate(), null);
        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);

        craneService.markAsMaintenance(
                maintenance.getCrane().getId()
        );

        return mapToResponse(maintenanceRepository.save(maintenance));
    }


    // Complete Maintenance
    @Transactional
    public MaintenanceResponse completeMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw BusinessException.conflict("Maintenance is already completed");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw BusinessException.conflict("Canceled maintenance cannot be completed");
        }

        schedulingService.lockCranes(maintenance.getCrane().getId());
        maintenance.setStatus(MaintenanceStatus.COMPLETED);

        craneService.markAsAvailable(
                maintenance.getCrane().getId()
        );

        return mapToResponse(maintenanceRepository.save(maintenance));
    }


    // Cancel Maintenance
    @Transactional
    public MaintenanceResponse cancelMaintenance(Long id) {

        Maintenance maintenance = maintenanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Maintenance not found"));

        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED) {
            throw BusinessException.conflict("Completed maintenance cannot be canceled");
        }

        if (maintenance.getStatus() == MaintenanceStatus.CANCELED) {
            throw BusinessException.conflict("Maintenance is already canceled");
        }

        schedulingService.lockCranes(maintenance.getCrane().getId());
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
