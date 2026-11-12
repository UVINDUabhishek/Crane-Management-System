package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
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
    private final SchedulingService schedulingService;

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
            TimesheetRepository timesheetRepository, SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
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
    @Transactional
    public JobCardResponse createJobCard(CreateJobCardRequest request) {

        SchedulingService.validateDates(request.getStartDate(), request.getEndDate());
        schedulingService.lockCranes(request.getCraneId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> BusinessException.notFound("Customer not found"));

        Quotation quotation = quotationRepository.findById(request.getQuotationId())
                .orElseThrow(() -> BusinessException.notFound("Quotation not found"));

        Crane crane = craneRepository.findById(request.getCraneId())
                .orElseThrow(() -> BusinessException.notFound("Crane not found"));

        // crane availability check
        if (crane.getStatus() != CraneStatus.AVAILABLE) {
            throw BusinessException.conflict("Crane is not available");
        }

        if (maintenanceService.isCraneUnderMaintenance(
                request.getCraneId(),
                request.getStartDate(),
                request.getEndDate())) {

            throw new BusinessException(HttpStatus.CONFLICT, "MAINTENANCE_OVERLAP",
                    "Crane is under maintenance during the selected period");
        }

        Operator operator = null;

        if (request.getOperatorId() != null) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> BusinessException.notFound("Operator not found"));
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User salesPerson = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        validateQuotation(customer, quotation, crane, request.getStartDate(), request.getEndDate());
        schedulingService.rejectBookingConflict(crane.getId(), request.getStartDate(), request.getEndDate(), null);

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
                        BusinessException.notFound("Job Card not found"));

        return mapToResponse(jobCard);
    }


    // Update Job-card
    @Transactional
    public JobCardResponse updateJobCard(Long id, UpdateJobCardRequest request) {

        JobCard jobCard = jobCardRepository.findByIdForUpdate(id)
                .orElseThrow(()-> BusinessException.notFound("Job card not found"));

        if (jobCard.getStatus() != JobCardStatus.PENDING) {
            throw BusinessException.conflict("Only pending job cards can be updated");
        }

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : jobCard.getStartDate();

        LocalDate endDate = request.isEndDateProvided()
                ? request.getEndDate()
                : jobCard.getEndDate();

        Long craneId = request.getCraneId() != null
                ? request.getCraneId()
                : jobCard.getCraneId().getId();

        SchedulingService.validateDates(startDate, endDate);
        schedulingService.lockCranes(jobCard.getCraneId().getId(), craneId);

        Crane crane = craneRepository.findById(craneId)
                .orElseThrow(() -> BusinessException.notFound("Crane not found"));

        if (crane.getStatus() != CraneStatus.AVAILABLE) {
            throw BusinessException.conflict("Crane is not available");
        }

        if (maintenanceService.isCraneUnderMaintenance(craneId, startDate, endDate)) {
            throw new BusinessException(HttpStatus.CONFLICT, "MAINTENANCE_OVERLAP",
                    "Crane is under maintenance during the selected period");
        }

        Quotation quotation = request.getQuotationId() == null ? jobCard.getQuotationId()
                : quotationRepository.findById(request.getQuotationId())
                    .orElseThrow(() -> BusinessException.notFound("Quotation not found"));
        validateQuotation(jobCard.getCustomerId(), quotation, crane, startDate, endDate);
        schedulingService.rejectBookingConflict(craneId, startDate, endDate, id);
        jobCard.setQuotationId(quotation);

        // Operator
        if(request.getOperatorId() != null) {

            Operator operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(()-> BusinessException.notFound("Operator not found"));

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

        jobCard.setCraneId(crane);

        JobCard updatedJobCard = jobCardRepository.save(jobCard);

        return mapToResponse(updatedJobCard);

    }


    // Complete Job Card
    @Transactional
    public JobCardResponse completeJobCard(Long id) {

        JobCard jobCard = jobCardRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        BusinessException.notFound("Job card not found"));

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
            throw BusinessException.conflict("Job card cannot be completed without timesheets");
        }

        // Minimum daily hours from quotation
        BigDecimal minHours = jobCard.getQuotationId().getMinHours();

        if(minHours == null) {
            throw BusinessException.conflict("Minimum hours are not defined in the quotation");
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


        schedulingService.lockCranes(jobCard.getCraneId().getId());

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
    @Transactional
    public JobCardResponse cancelJobCard(Long id) {

        JobCard jobCard = jobCardRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Job card not found"));

        if (jobCard.getStatus() == JobCardStatus.COMPLETED) {
            throw BusinessException.conflict("Completed job card cannot be cancelled");
        }

        if (jobCard.getStatus() == JobCardStatus.CANCELED) {
            throw BusinessException.conflict("Job card is already cancelled");
        }

        schedulingService.lockCranes(jobCard.getCraneId().getId());
        jobCard.setStatus(JobCardStatus.CANCELED);

        craneService.markAsAvailable(
                jobCard.getCraneId().getId()
        );

        JobCard canceledJobCard = jobCardRepository.save(jobCard);

        return mapToResponse(canceledJobCard);
    }


    private void validateQuotation(Customer customer, Quotation quotation, Crane crane,
                                   LocalDate start, LocalDate end) {

        if (!customer.isActive() || !quotation.isActive() || !crane.isActive()) {
            throw BusinessException.conflict("Customer, quotation and crane must be active");
        }
        if (!customer.getId().equals(quotation.getCustomerId().getId())) {
            throw BusinessException.invalid("Quotation must belong to the job customer");
        }
        if (!crane.getType().equals(quotation.getType())) {
            throw BusinessException.invalid("Crane type must match the quotation type");
        }
        // The current quotation API requires a finite validity window. A nullable DB column
        // alone does not establish an unbounded quotation contract.
        if (quotation.getValidFrom() == null || quotation.getValidTo() == null || end == null
                || start.isBefore(quotation.getValidFrom()) || end.isAfter(quotation.getValidTo())) {
            throw BusinessException.invalid("Job schedule must be entirely within quotation validity");
        }
    }

    // Mapping Method
    private JobCardResponse mapToResponse(JobCard jobCard) {

        JobCardResponse response = new JobCardResponse();
        response.setCustomerId(jobCard.getCustomerId().getId());
        response.setQuotationId(jobCard.getQuotationId().getId());
        response.setCraneId(jobCard.getCraneId().getId());
        response.setSalesPersonId(jobCard.getSalesPersonId().getId());

        response.setId(jobCard.getId());
        response.setJobCardNumber(jobCard.getJobCardNumber());
        response.setCustomer(jobCard.getCustomerId().getCompanyName());
        response.setQuotation(jobCard.getQuotationId().getQuotationNumber());
        response.setCrane(jobCard.getCraneId().getCraneNumber());
        response.setSalesPerson(jobCard.getSalesPersonId().getUsername());
        if (jobCard.getOperatorId() != null) {
            response.setOperatorId(jobCard.getOperatorId().getId());
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
