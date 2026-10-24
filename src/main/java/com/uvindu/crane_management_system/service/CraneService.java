package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.dto.CraneResponse;
import com.uvindu.crane_management_system.dto.CreateCraneRequest;
import com.uvindu.crane_management_system.dto.UpdateCraneRequest;
import com.uvindu.crane_management_system.exception.CraneAlreadyExistsException;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.repository.CraneRepository;
import org.springframework.stereotype.Service;
import com.uvindu.crane_management_system.repository.MaintenanceRepository;
import com.uvindu.crane_management_system.model.MaintenanceStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CraneService {

    private final CraneRepository craneRepository;
    private final MaintenanceRepository maintenanceRepository;

    public CraneService(CraneRepository craneRepository, MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.craneRepository = craneRepository;
    }

    // Create Crane
    public CraneResponse createCrane(CreateCraneRequest request) {

        if (craneRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new CraneAlreadyExistsException("Crane already exists.");
        }

        Crane crane = new Crane();

        crane.setCraneNumber(generateCraneNumber(request.getType()));
        crane.setType(request.getType());
        crane.setCapacity(request.getCapacity());
        crane.setManufacturer(request.getManufacturer());
        crane.setSerialNumber(request.getSerialNumber());
        crane.setYear(request.getYear());
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setLocation(request.getLocation());

        Crane savedCrane = craneRepository.save(crane);

        return mapToResponse(savedCrane);

    }


    // Generate crane number
    private String generateCraneNumber(String type) {

        Optional<Crane> latestCrane = craneRepository
                .findTopByTypeOrderByCraneNumberDesc(type);

        int nextNumber = 1;

        if (latestCrane.isPresent()) {
            String latestCraneNumber = latestCrane.get().getCraneNumber();

            String lastThreeDigits = latestCraneNumber.substring(
                    latestCraneNumber.length() - 3
            );

            nextNumber = Integer.parseInt(lastThreeDigits) + 1;
        }

        return String.format("CR%s%03d", type, nextNumber);
    }


    // Get All Cranes
    public List<CraneResponse> getAllCranes() {

        return craneRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // Get Crane by ID
    public CraneResponse getCraneById(Long id) {

        Crane crane = craneRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Crane not found"));

        return mapToResponse(crane);

    }


    // Update Crane
    public CraneResponse updateCrane(Long id, UpdateCraneRequest request) {

        Crane crane = craneRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Crane not found"));

        crane.setType(request.getType());
        crane.setCapacity(request.getCapacity());
        crane.setManufacturer(request.getManufacturer());
        crane.setSerialNumber(request.getSerialNumber());
        crane.setYear(request.getYear());
        crane.setLocation(request.getLocation());

        Crane updatedCrane = craneRepository.save(crane);

        return mapToResponse(updatedCrane);

    }

    // Mark Crane as Available
    public void markAsAvailable(Long id) {
        updateStatus(id, maintenanceRepository.existsByCrane_IdAndStatus(id, MaintenanceStatus.IN_PROGRESS)
                ? CraneStatus.MAINTENANCE : CraneStatus.AVAILABLE);
    }

    // Mark Crane as In-use
    public void markAsInUse(Long id) {
        updateStatus(id, CraneStatus.IN_USE);
    }

    // Mark Crane as Under Maintenance
    public void markAsMaintenance(Long id) {
        updateStatus(id, CraneStatus.MAINTENANCE);
    }

    // Update Crane Status
    private void updateStatus(Long id, CraneStatus status) {

        Crane crane = craneRepository.findById(id)
                .orElseThrow(()-> BusinessException.notFound("Crane not found"));

        crane.setStatus(status);

        craneRepository.save(crane);
    }


    // Delete Crane
    public void deleteCrane(Long id) {

        Crane crane = craneRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Crane not found"));

        crane.setActive(false);

        craneRepository.save(crane);

    }


    // Mapping Method
    private CraneResponse mapToResponse(Crane crane) {

        CraneResponse response = new CraneResponse();

        response.setId(crane.getId());
        response.setCraneNumber(crane.getCraneNumber());
        response.setType(crane.getType());
        response.setCapacity(crane.getCapacity());
        response.setManufacturer(crane.getManufacturer());
        response.setSerialNumber(crane.getSerialNumber());
        response.setYear(crane.getYear());
        response.setStatus(crane.getStatus());
        response.setLocation(crane.getLocation());

        return response;
    }

}
