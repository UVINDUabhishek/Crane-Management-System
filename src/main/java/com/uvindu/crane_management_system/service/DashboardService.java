package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.CraneIdleResponse;
import com.uvindu.crane_management_system.dto.DashboardResponse;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final CraneRepository craneRepository;
    private final JobCardRepository jobCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final TimesheetRepository timesheetRepository;

    public DashboardService(
            CraneRepository craneRepository,
            JobCardRepository jobCardRepository,
            InvoiceRepository invoiceRepository,
            MaintenanceRepository maintenanceRepository,
            TimesheetRepository timesheetRepository
    ) {
        this.craneRepository = craneRepository;
        this.jobCardRepository = jobCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.timesheetRepository = timesheetRepository;
    }

    // Get dashboard summary
    public DashboardResponse getDashboard() {

        DashboardResponse response = new DashboardResponse();

        //------------------- CRANE SUMMARY -------------------

        response.setTotalCranes(
                craneRepository.count()
        );

        response.setAvailableCranes(
                craneRepository.countByStatus(CraneStatus.AVAILABLE)
        );

        response.setInUseCranes(
                craneRepository.countByStatus(CraneStatus.IN_USE)
        );

        response.setMaintenanceCranes(
                craneRepository.countByStatus(CraneStatus.MAINTENANCE)
        );


        //------------------- JOB SUMMARY -------------------

        response.setPendingJobs(
                jobCardRepository.countByStatus(JobCardStatus.PENDING)
        );

        response.setCompletedJobs(
                jobCardRepository.countByStatus(JobCardStatus.COMPLETED)
        );

        response.setCanceledJobs(
                jobCardRepository.countByStatus(JobCardStatus.CANCELED)
        );

        //------------------- INVOICE SUMMARY -------------------

        response.setTotalInvoices(
                invoiceRepository.count()
        );

        response.setDraftInvoices(
                invoiceRepository.countByStatus(InvoiceStatus.DRAFT)
        );

        response.setCompletedInvoices(
                invoiceRepository.countByStatus(InvoiceStatus.COMPLETED)
        );

        response.setRevisedInvoices(
                invoiceRepository.countByStatus(InvoiceStatus.REVISED)
        );

        response.setCanceledInvoices(
                invoiceRepository.countByStatus(InvoiceStatus.CANCELED)
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<CraneIdleResponse> getCraneIdleStatistics(LocalDate fromDate, LocalDate toDate) {

        // Validate reporting period
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid reporting date range"
            );
        }

        // Historical statistics must not include future days.
        if (toDate.isAfter(LocalDate.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reporting end date cannot be in the future"
            );
        }

        // Prevent excessively large calendar calculations.
        if (ChronoUnit.DAYS.between(fromDate, toDate) > 365) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reporting period cannot exceed 366 days"
            );
        }

        List<Crane> cranes = craneRepository.findAll();

        // Retrieve relevant records in batches
        List<JobCard> reservations =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        List<Timesheet> timesheets =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        List<Maintenance> maintenanceRecords =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                        ),
                        fromDate,
                        toDate
                );

        // Group records by crane ID
        Map<Long, List<JobCard>> reservationsByCrane =
                reservations.stream()
                        .collect(Collectors.groupingBy(
                                job -> job.getCraneId().getId()
                        ));

        Map<Long, List<Timesheet>> timesheetsByCrane =
                timesheets.stream()
                        .collect(Collectors.groupingBy(
                                timesheet -> timesheet.getJobCard()
                                        .getCraneId().getId()
                        ));

        Map<Long, List<Maintenance>> maintenanceByCrane =
                maintenanceRecords.stream()
                        .collect(Collectors.groupingBy(
                                maintenance -> maintenance.getCrane().getId()
                        ));

        List<CraneIdleResponse> results = new ArrayList<>();

        for (Crane crane : cranes) {

            Long craneId = crane.getId();

            Set<LocalDate> reservedDates = new HashSet<>();
            Set<LocalDate> workedDates = new HashSet<>();
            Set<LocalDate> maintenanceDates = new HashSet<>();

            // Reserved days from pending jobs
            for (JobCard job : reservationsByCrane.getOrDefault(
                    craneId, List.of())) {

                addOverlappingDates(
                        reservedDates,
                        job.getStartDate(),
                        job.getEndDate(),
                        fromDate,
                        toDate
                );
            }

            // Worked days from completed-job timesheets
            for (Timesheet timesheet : timesheetsByCrane.getOrDefault(
                    craneId, List.of())) {

                workedDates.add(timesheet.getDateOfWork());
            }

            // Maintenance days
            for (Maintenance maintenance :
                    maintenanceByCrane.getOrDefault(craneId, List.of())) {

                addOverlappingDates(
                        maintenanceDates,
                        maintenance.getMaintenanceStartDate(),
                        maintenance.getMaintenanceEndDate(),
                        fromDate,
                        toDate
                );
            }

            // Maintenance has priority over worked and reserved dates.
            workedDates.removeAll(maintenanceDates);

            reservedDates.removeAll(maintenanceDates);
            reservedDates.removeAll(workedDates);

            long totalDays =
                    ChronoUnit.DAYS.between(fromDate, toDate) + 1;

            long maintenanceDays = maintenanceDates.size();
            long workedDays = workedDates.size();
            long reservedDays = reservedDates.size();

            long idleDays = totalDays
                    - maintenanceDays
                    - workedDays
                    - reservedDays;

            results.add(new CraneIdleResponse(
                    craneId,
                    crane.getCraneNumber(),
                    workedDays,
                    reservedDays,
                    maintenanceDays,
                    idleDays
            ));
        }

        results.sort(Comparator.comparing(
                CraneIdleResponse::getCraneNumber
        ));

        return results;
    }


    // Add inclusive dates intersecting a reporting period
    private void addOverlappingDates(
            Set<LocalDate> target,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (startDate == null) {
            return;
        }

        LocalDate effectiveEnd =
                endDate != null ? endDate : toDate;

        LocalDate start =
                startDate.isBefore(fromDate) ? fromDate : startDate;

        LocalDate end =
                effectiveEnd.isAfter(toDate) ? toDate : effectiveEnd;

        if (start.isAfter(end)) {
            return;
        }

        for (LocalDate date = start;
             !date.isAfter(end);
             date = date.plusDays(1)) {

            target.add(date);
        }
    }

}
