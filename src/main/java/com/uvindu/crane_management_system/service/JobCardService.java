package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.CreateJobCardRequest;
import com.uvindu.crane_management_system.dto.JobCardResponse;
import com.uvindu.crane_management_system.dto.UpdateJobCardRequest;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobCardService {

    private final JobCardRepository jobCardRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final CraneRepository craneRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final MaintenanceService maintenanceService;
    private final CraneService craneService;
    private final InvoiceService invoiceService;
    private final TimesheetRepository timesheetRepository;

    public JobCardService(
            JobCardRepository jobCardRepository,
            CustomerRepository customerRepository,
            QuotationRepository quotationRepository,
            CraneRepository craneRepository,
            UserRepository userRepository,
            OperatorRepository operatorRepository,
            MaintenanceService maintenanceService,
            CraneService craneService,
            InvoiceService invoiceService,
            TimesheetRepository timesheetRepository) {
        this.jobCardRepository = jobCardRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.craneRepository = craneRepository;
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.maintenanceService = maintenanceService;
        this.craneService = craneService;
        this.invoiceService = invoiceService;
        this.timesheetRepository = timesheetRepository;
    }

    // Create Job-card
    public JobCardResponse createJobCard(CreateJobCardRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Quotation quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> new RuntimeException("Crane not found"));

        // crane availability check
        if (crane.getStatus() != CraneStatus.AVAILABLE) {
            throw new RuntimeException(
                    "Crane is not available"
            );
        }

        if (maintenanceService.isCraneUnderMaintenance(
                request.getCraneId(),
                request.getStartDate(),
                request.getEndDate())) {

            throw new RuntimeException(
                    "Crane is under maintenance during the selected period"
            );
        }

        Operator operator = null;

        if (request.getOperatorId() != null) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new RuntimeException("Operator not found"));
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User salesPerson = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Reject an end date earlier than the start date
        if (request.getEndDate() != null &&
                request.getEndDate().isBefore(request.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }

        JobCard jobCard = new JobCard();

        jobCard.setJobCardNumber(generateJobCardNumber());
        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setOperatorId(operator);
        jobCard.setSalesPersonId(salesPerson);

        jobCard.setJobDate(LocalDate.now());
        jobCard.setStartDate(request.getStartDate());
        jobCard.setEndDate(request.getEndDate());
        jobCard.setLocation(request.getLocation());
        jobCard.setDescription(request.getDescription());
        jobCard.setStatus(JobCardStatus.PENDING);

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        return mapToResponse(savedJobCard);
    }

    // Generate Job-card number
    private String generateJobCardNumber() {

        Optional<JobCard> latestJobCard =
                jobCardRepository.findTopByOrderByIdDesc();

        if (latestJobCard.isEmpty()) {
            return "JC-0001";
        }

        String latestNumber = latestJobCard.get().getJobCardNumber();

        int number = Integer.parseInt(latestNumber.substring(3));

        return String.format("JC-%04d", number + 1);
    }

    // Get All Job-cards
    public List<JobCardResponse> getAllJobCards() {

        return jobCardRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get Job-card by ID
    public JobCardResponse getJobCardById(Long id) {

        JobCard jobCard = jobCardRepository.findById(id)
                .orElseThrow(()->
                        new RuntimeException("Job Card not found"));

        return mapToResponse(jobCard);
    }


    // Update Job-card
    public JobCardResponse updateJobCard(Long id, UpdateJobCardRequest request) {

        JobCard jobCard = jobCardRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Job card not found"));

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : jobCard.getStartDate();

        LocalDate endDate = request.getEndDate() != null
                ? request.getEndDate()
                : jobCard.getEndDate();

        Long craneId = request.getCraneId() != null
                ? request.getCraneId()
                : jobCard.getCraneId().getId();

        if (endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("End date cannot be before start date");
        }

        Crane crane = craneRepository.findById(craneId)
                .orElseThrow(() -> new RuntimeException("Crane not found"));

        if (crane.getStatus() != CraneStatus.AVAILABLE) {
            throw new RuntimeException("Crane is not available");
        }

        if (startDate != null && endDate != null) {

            if (maintenanceService.isCraneUnderMaintenance(
                    craneId,
                    startDate,
                    endDate)) {

                throw new RuntimeException(
                        "Crane is under maintenance during the selected period"
                );
            }
        }

        // Quotation
        if(request.getQuotationId() != null) {

            Quotation quotation = quotationRepository.findById(request.getQuotationId())
                    .orElseThrow(()-> new RuntimeException("Quotation not found"));

                jobCard.setQuotationId(quotation);
        }


        // Operator
        if(request.getOperatorId() != null) {

            Operator operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(()-> new RuntimeException("Operator not found"));

            jobCard.setOperatorId(operator);
        }

        // Start Date
        jobCard.setStartDate(startDate);

        // End Date
        jobCard.setEndDate(endDate);

        // Location
        if(request.getLocation() != null) {
            jobCard.setLocation(request.getLocation());
        }

        // Description
        if(request.getDescription() != null) {
            jobCard.setDescription(request.getDescription());
        }

        JobCard updatedJobCard = jobCardRepository.save(jobCard);

        return mapToResponse(updatedJobCard);

    }


    // Complete Job Card
    @Transactional
    public JobCardResponse completeJobCard(Long id) {

        JobCard jobCard = jobCardRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Job card not found"));

        // Only PENDING job cards can be completed
        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending job cards can be completed"
            );
        }

        // Get all timesheets for this Job-card
        List<Timesheet> timesheets = timesheetRepository.findByJobCard(jobCard);

        //Timesheet is required before completion
        if(timesheets.isEmpty()) {
            throw new RuntimeException(
                    "Job card cannot be completed without timesheets"
            );
        }

        // Minimum daily hours from quotation
        BigDecimal minHours = jobCard.getQuotationId().getMinHours();

        if(minHours == null) {
            throw new RuntimeException(
                    "Minimum hours are not defined in the quotation"
            );
        }

        BigDecimal totalWorkedHours = BigDecimal.ZERO;
        BigDecimal totalBillableHours = BigDecimal.ZERO;

        // Calculate actual and billable hours
        for(Timesheet timesheet : timesheets) {

            BigDecimal workedHours = timesheet.getWorkedHours();

            totalWorkedHours = totalWorkedHours.add(workedHours);

            BigDecimal billableHours = workedHours.max(minHours);

            totalBillableHours = totalBillableHours.add(billableHours);
        }


        // Store calculated totals on Job-card
        jobCard.setTotalWorkedHours(totalWorkedHours);
        jobCard.setTotalBillableHours(totalBillableHours);

        // Complete Job-card
        jobCard.setStatus(JobCardStatus.COMPLETED);

        // Make crane available
        craneService.markAsAvailable(
                jobCard.getCraneId().getId()
        );

        JobCard completedJobCard = jobCardRepository.save(jobCard);

        // Automatically create Invoice
        invoiceService.createInvoiceFromJobCard(completedJobCard);

        return mapToResponse(completedJobCard);
    }


    // Cancel Job-card
    public JobCardResponse cancelJobCard(Long id) {

        JobCard jobCard = jobCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job card not found"));

        if (jobCard.getStatus() == JobCardStatus.COMPLETED) {
            throw new RuntimeException("Completed job card cannot be cancelled");
        }

        if (jobCard.getStatus() == JobCardStatus.CANCELED) {
            throw new RuntimeException("Job card is already cancelled");
        }

        jobCard.setStatus(JobCardStatus.CANCELED);

        craneService.markAsAvailable(
                jobCard.getCraneId().getId()
        );

        JobCard canceledJobCard = jobCardRepository.save(jobCard);

        return mapToResponse(canceledJobCard);
    }


    // Mapping Method
    private JobCardResponse mapToResponse(JobCard jobCard) {

        JobCardResponse response = new JobCardResponse();

        response.setId(jobCard.getId());
        response.setJobCardNumber(jobCard.getJobCardNumber());
        response.setCustomer(jobCard.getCustomerId().getCompanyName());
        response.setQuotation(jobCard.getQuotationId().getQuotationNumber());
        response.setCrane(jobCard.getCraneId().getCraneNumber());
        response.setSalesPerson(jobCard.getSalesPersonId().getUsername());
        if (jobCard.getOperatorId() != null) {
            response.setOperator(jobCard.getOperatorId().getOperatorName());
        }
        response.setJobDate(jobCard.getJobDate());
        response.setStartDate(jobCard.getStartDate());
        response.setEndDate(jobCard.getEndDate());
        response.setLocation(jobCard.getLocation());
        response.setDescription(jobCard.getDescription());
        response.setTotalWorkedHours(jobCard.getTotalWorkedHours());
        response.setTotalBillableHours(jobCard.getTotalBillableHours());
        response.setStatus(jobCard.getStatus());

        return response;
    }

}
