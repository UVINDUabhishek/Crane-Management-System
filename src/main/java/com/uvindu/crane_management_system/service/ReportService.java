package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.InvoiceRepository;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import com.uvindu.crane_management_system.repository.MaintenanceRepository;
import com.uvindu.crane_management_system.repository.TimesheetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportService {

    private final JobCardRepository jobCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final TimesheetRepository timesheetRepository;
    private final MaintenanceRepository maintenanceRepository;

    public ReportService(JobCardRepository jobCardRepository,
                         InvoiceRepository invoiceRepository,
                         TimesheetRepository timesheetRepository,
                         MaintenanceRepository maintenanceRepository) {
        this.jobCardRepository = jobCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.timesheetRepository = timesheetRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    // ------------- START JOB-CARD REPORTS ---------------------

    // Get all jobs-cards or filter by date range
    public List<JobCardReportResponse> getJobCardReport(LocalDate fromDate, LocalDate toDate) {

            List<JobCard> jobCards;

            if(fromDate != null && toDate != null) {

                jobCards = jobCardRepository.findByJobDateBetween(
                        fromDate,
                        toDate
                );
            } else {

                jobCards = jobCardRepository.findAll();
            }

            return jobCards.stream()
                    .map(this::mapToJobCardReportResponse)
                    .toList();
    }


    // Get job-cards by customer
    public List<JobCardReportResponse> getJobCardReportByCustomer(Long customerId) {

        List<JobCard> jobCards = jobCardRepository.findByCustomerId_Id(customerId);

        return jobCards.stream()
                .map(this::mapToJobCardReportResponse)
                .toList();
    }

    // Get job-cards by crane
    public List<JobCardReportResponse> getJobCardReportByCrane(Long craneId) {

        List<JobCard> jobCards = jobCardRepository.findByCraneId_Id(craneId);

        return jobCards.stream()
                .map(this::mapToJobCardReportResponse)
                .toList();
    }

    // Get job-cards by status
    public List<JobCardReportResponse> getJobCardReportByStatus(JobCardStatus status) {

        List<JobCard> jobCards = jobCardRepository.findByStatus(status);

        return jobCards.stream()
                .map(this::mapToJobCardReportResponse)
                .toList();
    }

    // Get job-cards by sales person
    public List<JobCardReportResponse> getJobCardReportBySalesPerson(Long salesPersonId) {

        List<JobCard> jobCards = jobCardRepository.findBySalesPersonId_Id(salesPersonId);

        return jobCards.stream()
                .map(this::mapToJobCardReportResponse)
                .toList();
    }

    // Mapping method -> job-card
    private JobCardReportResponse mapToJobCardReportResponse(JobCard jobCard) {

        JobCardReportResponse response = new JobCardReportResponse();

        response.setJobCardNumber(jobCard.getJobCardNumber());

        response.setCustomerName(
                jobCard.getCustomerId().getCompanyName()
        );

        response.setCraneNumber(
                jobCard.getCraneId().getCraneNumber()
        );

        response.setSalesPerson(
                jobCard.getSalesPersonId().getUsername()
        );

        if (jobCard.getOperatorId() != null) {
            response.setOperatorName(
                    jobCard.getOperatorId().getOperatorName()
            );
        }

        response.setJobDate(jobCard.getJobDate());
        response.setStartDate(jobCard.getStartDate());
        response.setEndDate(jobCard.getEndDate());
        response.setLocation(jobCard.getLocation());

        response.setTotalWorkedHours(
                jobCard.getTotalWorkedHours()
        );

        response.setTotalBillableHours(
                jobCard.getTotalBillableHours()
        );

        response.setStatus(
                jobCard.getStatus().name()
        );

        return response;
    }

    // ------------- END JOB-CARD REPORTS ---------------------



    // ------------- START INVOICE REPORTS ---------------------


    // Get all invoices
    public List<InvoiceReportResponse> getInvoiceReport() {

        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .map(this::mapToInvoiceReportResponse)
                .toList();
    }

    // Invoice Filter by date
    public List<InvoiceReportResponse> getInvoiceReportByDate(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<Invoice> invoices =
                invoiceRepository.findByInvoiceDateBetween(
                        fromDate,
                        toDate
                );

        return invoices.stream()
                .map(this::mapToInvoiceReportResponse)
                .toList();
    }

    // Invoice filter by customer
    public List<InvoiceReportResponse> getInvoiceReportByCustomer(
            Long customerId
    ) {

        List<Invoice> invoices =
                invoiceRepository.findByCustomer_Id(customerId);

        return invoices.stream()
                .map(this::mapToInvoiceReportResponse)
                .toList();
    }

    // Invoice filter by status
    public List<InvoiceReportResponse> getInvoiceReportByStatus(
            InvoiceStatus status
    ) {

        List<Invoice> invoices =
                invoiceRepository.findByStatus(status);

        return invoices.stream()
                .map(this::mapToInvoiceReportResponse)
                .toList();
    }

    // Mapping method -> Invoice
    private InvoiceReportResponse mapToInvoiceReportResponse(Invoice invoice) {

        InvoiceReportResponse response = new InvoiceReportResponse();

        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setCustomerName(invoice.getCustomer().getCompanyName());
        response.setCraneNumber(invoice.getCrane().getCraneNumber());
        response.setJobCardNumber(invoice.getJobCard().getJobCardNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setQty(invoice.getQty());
        response.setRate(invoice.getRate());
        response.setAmount(invoice.getAmount());
        response.setMobilization(invoice.getMobilization());
        response.setDescription(invoice.getDescription());
        response.setStatus(invoice.getStatus().name());

        return response;
    }

    // ------------- END INVOICE REPORTS ---------------------




    // ------------- START UTILIZATION REPORTS ---------------------

    public List<CraneUtilizationReportResponse> getCraneUtilizationReport() {

        List<Timesheet> timesheets = timesheetRepository.findAllWithJobCardAndCrane();

        Map<Long, CraneUtilizationReportResponse> reportMap = new LinkedHashMap<>();

        for (Timesheet timesheet : timesheets) {

            JobCard jobCard = timesheet.getJobCard();
            Crane crane = jobCard.getCraneId();

            CraneUtilizationReportResponse response =
                    reportMap.computeIfAbsent(
                            crane.getId(),
                            id -> {
                                CraneUtilizationReportResponse newResponse = new CraneUtilizationReportResponse();

                                newResponse.setCraneNumber(crane.getCraneNumber());
                                newResponse.setCraneType(crane.getType());
                                newResponse.setTotalJobs(0);
                                newResponse.setTotalWorkedHours(BigDecimal.ZERO);

                                return newResponse;
                            }
                    );

            response.setTotalWorkedHours(
                    response.getTotalWorkedHours()
                            .add(timesheet.getWorkedHours())
            );
        }

        // Count unique Job Cards for each crane
        Map<Long, Set<Long>> craneJobCards = new HashMap<>();

        for (Timesheet timesheet : timesheets) {

            JobCard jobCard = timesheet.getJobCard();
            Crane crane = jobCard.getCraneId();

            craneJobCards
                    .computeIfAbsent(
                            crane.getId(),
                            id -> new HashSet<>()
                    )
                    .add(jobCard.getId());
        }

        for (Map.Entry<Long, Set<Long>> entry :
                craneJobCards.entrySet()) {

            CraneUtilizationReportResponse response =
                    reportMap.get(entry.getKey());

            response.setTotalJobs(
                    entry.getValue().size()
            );
        }

        return new ArrayList<>(reportMap.values());
    }

    // ------------- END UTILIZATION REPORTS ---------------------



    // ------------- START REVENUE SUMMARY REPORTS ---------------------

    // Get revenue summary
    public RevenueSummaryResponse getRevenueSummary() {

        List<Invoice> invoices =
                invoiceRepository.findByStatus(InvoiceStatus.COMPLETED);

        return calculateRevenueSummary(invoices);
    }

    // Get revenue summary by date
    public RevenueSummaryResponse getRevenueSummaryByDate(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<Invoice> invoices =
                invoiceRepository.findByStatusAndInvoiceDateBetween(
                        InvoiceStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        return calculateRevenueSummary(invoices);
    }

    // Calculation method
    private RevenueSummaryResponse calculateRevenueSummary(List<Invoice> invoices) {

        RevenueSummaryResponse response = new RevenueSummaryResponse();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalMobilization = BigDecimal.ZERO;

        for(Invoice invoice : invoices) {

            if(invoice.getAmount() != null) {
                totalAmount = totalAmount.add(
                        invoice.getAmount()
                );
            }

            if(invoice.getMobilization() != null) {
                totalMobilization = totalMobilization.add(
                        invoice.getMobilization()
                );
            }
        }

        response.setTotalInvoices(
                (long) invoices.size()
        );

        response.setTotalAmount(totalAmount);
        response.setTotalMobilization(totalMobilization);
        // Invoice amount already includes mobilization. Keep the separate breakdown informational.
        response.setTotalRevenue(totalAmount);

        return response;
    }

    // Get monthly revenue report
    public List<MonthlyRevenueReportResponse> getMonthlyRevenueReport() {

        List<Invoice> invoices =
                invoiceRepository.findByStatus(InvoiceStatus.COMPLETED);

        Map<String, MonthlyRevenueReportResponse> reportMap =
                new LinkedHashMap<>();

        for(Invoice invoice : invoices) {

            LocalDate invoiceDate = invoice.getInvoiceDate();

            Integer year = invoiceDate.getYear();
            Integer month = invoiceDate.getMonthValue();

            String key = year + "-" + month;

            MonthlyRevenueReportResponse response =
                    reportMap.computeIfAbsent(
                            key, id->{
                                MonthlyRevenueReportResponse newResponse =
                                        new MonthlyRevenueReportResponse();

                                newResponse.setYear(year);
                                newResponse.setMonth(month);

                                // January instead of JANUARY
                                newResponse.setMonthName(
                                        invoiceDate.getMonth()
                                                .getDisplayName(
                                                        TextStyle.FULL,
                                                        Locale.ENGLISH
                                                )
                                );

                                newResponse.setTotalAmount(BigDecimal.ZERO);
                                newResponse.setTotalMobilization(BigDecimal.ZERO);
                                newResponse.setTotalRevenue(BigDecimal.ZERO);

                                newResponse.setTotalInvoices(0L);

                                return newResponse;
                            }
                    );

            if(invoice.getAmount() != null) {

                response.setTotalAmount(
                        response.getTotalAmount()
                                .add(invoice.getAmount())
                );
            }

            if(invoice.getMobilization() != null) {

                response.setTotalMobilization(
                        response.getTotalMobilization()
                                .add(invoice.getMobilization())
                );
            }

            response.setTotalInvoices(
                    response.getTotalInvoices() + 1
            );

            // Invoice amount already includes mobilization.
            response.setTotalRevenue(response.getTotalAmount());
        }

        return new ArrayList<>(reportMap.values());
    }

    // Get Revenue summary by customer
    public List<CustomerRevenueReportResponse> getCustomerRevenueReport() {

        List<Invoice> invoices =
                invoiceRepository.findByStatus(InvoiceStatus.COMPLETED);

        Map<Long, CustomerRevenueReportResponse> reportMap =
                new LinkedHashMap<>();

        for(Invoice invoice : invoices) {

            Long customerId = invoice.getCustomer().getId();

            CustomerRevenueReportResponse response =
                    reportMap.computeIfAbsent(
                            customerId,
                            id-> {

                                CustomerRevenueReportResponse newResponse =
                                        new CustomerRevenueReportResponse();

                                newResponse.setCustomerId(customerId);

                                newResponse.setCustomerName(
                                        invoice.getCustomer().getCompanyName()
                                );

                                newResponse.setTotalInvoices(0L);

                                newResponse.setTotalAmount(
                                        BigDecimal.ZERO
                                );

                                newResponse.setTotalMobilization(
                                        BigDecimal.ZERO
                                );

                                newResponse.setTotalRevenue(
                                        BigDecimal.ZERO
                                );

                                return newResponse;
                            }
                    );

            if(invoice.getAmount() != null) {

                response.setTotalAmount(
                        response.getTotalAmount().add(invoice.getAmount())
                );
            }

            if(invoice.getMobilization() != null) {

                response.setTotalMobilization(
                        response.getTotalMobilization().add(invoice.getMobilization())
                );
            }

            response.setTotalInvoices(
                    response.getTotalInvoices() + 1
            );

            // Invoice amount already includes mobilization.
            response.setTotalRevenue(response.getTotalAmount());
        }

        return new ArrayList<>(reportMap.values());
    }

    // Get revenue by crane
    public List<CraneRevenueReportResponse> getCraneRevenueReport() {

        List<Invoice> invoices =
                invoiceRepository.findByStatus(InvoiceStatus.COMPLETED);

        Map<Long, CraneRevenueReportResponse> reportMap =
                new LinkedHashMap<>();

        for(Invoice invoice : invoices) {

            Long craneId = invoice.getCrane().getId();

            CraneRevenueReportResponse response =
                    reportMap.computeIfAbsent(
                            craneId,
                            id-> {
                                CraneRevenueReportResponse newResponse =
                                        new CraneRevenueReportResponse();

                                newResponse.setCraneId(craneId);

                                newResponse.setCraneNumber(
                                        invoice.getCrane().getCraneNumber()
                                );

                                newResponse.setCraneType(
                                        invoice.getCrane().getType()
                                );

                                newResponse.setTotalInvoices(0L);

                                newResponse.setTotalAmount(BigDecimal.ZERO);
                                newResponse.setTotalMobilization(BigDecimal.ZERO);
                                newResponse.setTotalRevenue(BigDecimal.ZERO);

                                return newResponse;
                            }
                    );

            if(invoice.getAmount() != null) {

                response.setTotalAmount(
                        response.getTotalAmount()
                                .add(invoice.getAmount())
                );
            }

            if(invoice.getMobilization() != null) {

                response.setTotalMobilization(
                        response.getTotalMobilization()
                                .add(invoice.getMobilization())
                );
            }

            response.setTotalInvoices(
                    response.getTotalInvoices() + 1
            );

            // Invoice amount already includes mobilization.
            response.setTotalRevenue(response.getTotalAmount());

        }

        return new ArrayList<>(reportMap.values());
    }

    // ------------- END REVENUE SUMMARY REPORTS ---------------------



    // ------------- START MAINTENANCE REPORTS ---------------------

    // Maintenance report
    public List<MaintenanceReportResponse> getMaintenanceReport() {

        List<Maintenance> maintenances =
                maintenanceRepository.findAll();

        List<MaintenanceReportResponse> responseList =
                new ArrayList<>();

        for (Maintenance maintenance : maintenances) {

            MaintenanceReportResponse response =
                    new MaintenanceReportResponse();

            response.setMaintenanceNumber(
                    maintenance.getMaintenanceNumber()
            );

            response.setCraneNumber(
                    maintenance.getCrane().getCraneNumber()
            );

            response.setCraneType(
                    maintenance.getCrane().getType()
            );

            response.setMaintenanceDate(
                    maintenance.getMaintenanceDate()
            );

            response.setStartDate(
                    maintenance.getMaintenanceStartDate()
            );

            response.setEndDate(
                    maintenance.getMaintenanceEndDate()
            );

            response.setRepairType(
                    maintenance.getMaintenanceType().name()
            );

            response.setMaintenancePerson(
                    maintenance.getMaintenancePerson() == null
                            ? null : maintenance.getMaintenancePerson().getUsername()
            );

            response.setStatus(
                    maintenance.getStatus().name()
            );

            responseList.add(response);
        }

        return responseList;
    }


    // Crane maintenance summary
    public List<CraneMaintenanceSummaryResponse> getCraneMaintenanceSummary() {

        List<Maintenance> maintenances =
                maintenanceRepository.findAll();

        Map<Long, CraneMaintenanceSummaryResponse> reportMap =
                new LinkedHashMap<>();

        for (Maintenance maintenance : maintenances) {

            Long craneId = maintenance.getCrane().getId();

            CraneMaintenanceSummaryResponse response =
                    reportMap.computeIfAbsent(
                            craneId,
                            id -> {

                                CraneMaintenanceSummaryResponse newResponse =
                                        new CraneMaintenanceSummaryResponse();

                                newResponse.setCraneId(craneId);

                                newResponse.setCraneNumber(
                                        maintenance.getCrane().getCraneNumber()
                                );

                                newResponse.setCraneType(
                                        maintenance.getCrane().getType()
                                );

                                newResponse.setTotalMaintenanceRecords(0L);

                                newResponse.setTotalMaintenanceDays(0L);

                                return newResponse;
                            }
                    );

            response.setTotalMaintenanceRecords(
                    response.getTotalMaintenanceRecords() + 1
            );

            if (maintenance.getMaintenanceStartDate() != null
                    && maintenance.getMaintenanceEndDate() != null) {

                long maintenanceDays =
                        ChronoUnit.DAYS.between(
                                maintenance.getMaintenanceStartDate(),
                                maintenance.getMaintenanceEndDate()
                        ) + 1;

                response.setTotalMaintenanceDays(
                        response.getTotalMaintenanceDays()
                                + maintenanceDays
                );
            }
        }

        return new ArrayList<>(reportMap.values());
    }

    // Get maintenance report by status
    public List<MaintenanceStatusReportResponse> getMaintenanceStatusReport() {

        List<Maintenance> maintenances =
                maintenanceRepository.findAll();

        Map<MaintenanceStatus, Long> statusMap =
                new LinkedHashMap<>();

        // Initialize all statuses
        for (MaintenanceStatus status : MaintenanceStatus.values()) {
            statusMap.put(status, 0L);
        }

        // Count maintenance records
        for (Maintenance maintenance : maintenances) {

            MaintenanceStatus status =
                    maintenance.getStatus();

            statusMap.put(
                    status,
                    statusMap.get(status) + 1
            );
        }

        List<MaintenanceStatusReportResponse> responseList =
                new ArrayList<>();

        for (Map.Entry<MaintenanceStatus, Long> entry
                : statusMap.entrySet()) {

            MaintenanceStatusReportResponse response =
                    new MaintenanceStatusReportResponse();

            response.setStatus(
                    entry.getKey().name()
            );

            response.setTotal(
                    entry.getValue()
            );

            responseList.add(response);
        }

        return responseList;
    }

    // ------------- END MAINTENANCE REPORTS -----------------------


}
