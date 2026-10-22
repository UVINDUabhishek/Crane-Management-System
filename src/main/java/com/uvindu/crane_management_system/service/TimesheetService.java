package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.CreateTimesheetRequest;
import com.uvindu.crane_management_system.dto.TimesheetResponse;
import com.uvindu.crane_management_system.dto.UpdateTimesheetRequest;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Timesheet;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import com.uvindu.crane_management_system.repository.TimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final JobCardRepository jobCardRepository;

    public TimesheetService(TimesheetRepository timesheetRepository,
                            JobCardRepository jobCardRepository) {
        this.timesheetRepository = timesheetRepository;
        this.jobCardRepository = jobCardRepository;
    }

    // Create timesheet
    public TimesheetResponse createTimesheet(CreateTimesheetRequest request) {

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(()-> new RuntimeException("Job card not found"));

        // A completed or canceled Job-card cannot receive new Timesheets
        if(jobCard.getStatus() != JobCardStatus.PENDING) {
            throw new RuntimeException(
                    "Timesheet can only be added to a pending Job Card"
            );
        }

        // Prevent duplicate Timesheet for the same Job Card and date
        if (timesheetRepository.existsByJobCardAndDateOfWork(
                jobCard,
                request.getDateOfWork())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Timesheet already exists for this Job Card on this date"
            );
        }

        Timesheet timesheet = new Timesheet();

        timesheet.setTimesheetNumber(generateTimesheetNumber());
        timesheet.setJobCard(jobCard);
        timesheet.setDateOfWork(request.getDateOfWork());
        timesheet.setWorkedHours(request.getWorkedHours());
        timesheet.setDescription(request.getDescription());

        Timesheet savedTimesheet = timesheetRepository.save(timesheet);

        return mapToResponse(savedTimesheet);
    }


    // Generate timesheet number
    private String generateTimesheetNumber() {

        Optional<Timesheet> latestTimesheet =
                timesheetRepository.findTopByOrderByIdDesc();

        if (latestTimesheet.isEmpty()) {
            return "TS-0001";
        }

        String latestNumber = latestTimesheet.get().getTimesheetNumber();

        int number = Integer.parseInt(latestNumber.substring(3));

        return String.format("TS-%04d", number + 1);
    }

    // Get all timesheets
    public List<TimesheetResponse> getAllTimesheets() {

        return timesheetRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get timesheet by ID
    public TimesheetResponse getTimesheetById(Long id) {

        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Timesheet not found"));

        return mapToResponse(timesheet);
    }

    // Get timesheets for specific Job-card
    public List<TimesheetResponse> getTimesheetsByJobCard(Long jobCardId) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(()-> new RuntimeException("Job card not found"));

        return timesheetRepository.findByJobCard(jobCard)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Update timesheet
    public TimesheetResponse updateTimesheet(Long id, UpdateTimesheetRequest request) {

        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet not found"));

        JobCard jobCard = timesheet.getJobCard();

        // Timesheet can only be updated while Job Card is PENDING
        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw new RuntimeException(
                    "Timesheet cannot be updated because the Job Card is not pending"
            );
        }

        if(request.getDateOfWork() != null) {

            // Check if another Timesheet already uses this date
            Optional<Timesheet> existingTimesheet = timesheetRepository.findByJobCardAndDateOfWork(
                    jobCard,
                    request.getDateOfWork()
            );

            if (existingTimesheet.isPresent()
                    && !existingTimesheet.get().getId().equals(id)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Timesheet already exists for this Job Card on this date"
                );
            }

            timesheet.setDateOfWork(request.getDateOfWork());
        }

        if(request.getWorkedHours() != null) {
            timesheet.setWorkedHours(request.getWorkedHours());
        }

        if(request.getDescription() != null) {
            timesheet.setDescription(request.getDescription());
        }

        Timesheet updatedTimesheet = timesheetRepository.save(timesheet);

        return mapToResponse(updatedTimesheet);
    }


    // Delete timesheet
    public void deleteTimesheet(Long id) {

        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timesheet not found"));

        JobCard jobCard = timesheet.getJobCard();

        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw new RuntimeException(
                    "Timesheet cannot be deleted because the Job Card is not pending"
            );
        }

        timesheetRepository.delete(timesheet);
    }

    // Mapping Method
    private TimesheetResponse mapToResponse(Timesheet timesheet) {

        TimesheetResponse response = new TimesheetResponse();
        response.setId(timesheet.getId());
        response.setJobCardId(timesheet.getJobCard().getId());
        response.setCustomerId(timesheet.getJobCard().getCustomerId().getId());

        response.setTimesheetNumber(timesheet.getTimesheetNumber());

        response.setJobCardNumber(
                timesheet.getJobCard().getJobCardNumber()
        );

        response.setCustomerName(
                timesheet.getJobCard().getCustomerId().getCompanyName()
        );

        response.setDateOfWork(timesheet.getDateOfWork());
        response.setWorkedHours(timesheet.getWorkedHours());
        response.setDescription(timesheet.getDescription());

        return response;
    }

}
