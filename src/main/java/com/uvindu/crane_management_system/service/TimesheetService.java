package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
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
import java.time.LocalDate;
import java.time.ZoneId;
import jakarta.transaction.Transactional;
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
    @Transactional
    public TimesheetResponse createTimesheet(CreateTimesheetRequest request) {

        JobCard jobCard = jobCardRepository.findByIdForUpdate(request.getJobCardId())
                .orElseThrow(()-> BusinessException.notFound("Job card not found"));

        // A completed or canceled Job-card cannot receive new Timesheets
        if(jobCard.getStatus() != JobCardStatus.PENDING) {
            throw BusinessException.conflict("Timesheet can only be added to a pending Job Card");
        }

        validateWorkDate(request.getDateOfWork());

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
                .orElseThrow(()-> BusinessException.notFound("Timesheet not found"));

        return mapToResponse(timesheet);
    }

    // Get timesheets for specific Job-card
    public List<TimesheetResponse> getTimesheetsByJobCard(Long jobCardId) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(()-> BusinessException.notFound("Job card not found"));

        return timesheetRepository.findByJobCard(jobCard)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Update timesheet
    @Transactional
    public TimesheetResponse updateTimesheet(Long id, UpdateTimesheetRequest request) {

        Long jobId = timesheetRepository.findJobCardIdById(id)
                .orElseThrow(() -> BusinessException.notFound("Timesheet not found"));
        JobCard jobCard = jobCardRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> BusinessException.notFound("Job card not found"));

        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Timesheet not found"));



        // Timesheet can only be updated while Job Card is PENDING
        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw BusinessException.conflict("Timesheet cannot be updated because the Job Card is not pending");
        }

        validateWorkDate(request.getDateOfWork());

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
    @Transactional
    public void deleteTimesheet(Long id) {

        Long jobId = timesheetRepository.findJobCardIdById(id)
                .orElseThrow(() -> BusinessException.notFound("Timesheet not found"));
        JobCard jobCard = jobCardRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> BusinessException.notFound("Job card not found"));

        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Timesheet not found"));



        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw BusinessException.conflict("Timesheet cannot be deleted because the Job Card is not pending");
        }

        timesheetRepository.delete(timesheet);
    }

    private void validateWorkDate(LocalDate date) {
        // Same business-local date as the browser; do not convert a LocalDate through UTC.
        if (date != null && date.isAfter(LocalDate.now(ZoneId.of("Asia/Colombo")))) {
            throw BusinessException.invalid("Date of work cannot be later than today (Asia/Colombo)");
        }
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
