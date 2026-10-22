package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    // ------------- START JOB-CARD REPORTS ---------------------

    // Get all jobs-cards or filter by date range
    @GetMapping("/job-cards")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public ResponseEntity<List<JobCardReportResponse>> getJobCardReport(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        return ResponseEntity.ok(reportService.getJobCardReport(fromDate, toDate));
    }

    // Get job-cards by customer
    @GetMapping("/job-cards/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public ResponseEntity<List<JobCardReportResponse>> getJobCardReportByCustomer(
            @PathVariable Long customerId
    ) {

        return ResponseEntity.ok(
                reportService.getJobCardReportByCustomer(customerId)
        );
    }

    // Get job-cards by crane
    @GetMapping("/job-cards/crane/{craneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public ResponseEntity<List<JobCardReportResponse>> getJobCardReportByCrane(
            @PathVariable Long craneId
    ) {

        return ResponseEntity.ok(
                reportService.getJobCardReportByCrane(craneId)
        );
    }

    // Get job-cards by status
    @GetMapping("/job-cards/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public ResponseEntity<List<JobCardReportResponse>> getJobCardReportByStatus(
            @PathVariable JobCardStatus status
    ) {

        return ResponseEntity.ok(
                reportService.getJobCardReportByStatus(status)
        );
    }

    // Get job-cards by sales person
    @GetMapping("/job-cards/sales-person/{salesPersonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS', 'FINANCE')")
    public ResponseEntity<List<JobCardReportResponse>> getJobCardReportBySalesPerson(
            @PathVariable Long salesPersonId
    ) {

        return ResponseEntity.ok(
                reportService.getJobCardReportBySalesPerson(salesPersonId)
        );
    }

    // ------------- END JOB-CARD REPORTS -----------------------


    // ------------- START INVOICE REPORTS ----------------------

    // Get all invoices
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<List<InvoiceReportResponse>> getInvoiceReport() {

        return ResponseEntity.ok(reportService.getInvoiceReport());
    }

    // Invoice Filter by date
    @GetMapping("/invoices/date")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<List<InvoiceReportResponse>> getInvoiceReportByDate(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {

        return ResponseEntity.ok(reportService.getInvoiceReportByDate(fromDate, toDate));
    }

    // Invoice filter by customer
    @GetMapping("/invoices/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<List<InvoiceReportResponse>> getInvoiceReportByCustomer(@PathVariable Long customerId) {

        return ResponseEntity.ok(reportService.getInvoiceReportByCustomer(customerId));
    }

    // Invoice filter by status
    @GetMapping("/invoices/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<List<InvoiceReportResponse>> getInvoiceReportByStatus(@PathVariable InvoiceStatus status) {

        return ResponseEntity.ok(reportService.getInvoiceReportByStatus(status));
    }

    // ------------- END INVOICE REPORTS ---------------------



    // ------------- START CRANE-UTILIZATION REPORTS ---------------------

    // Get crane-utilization
    @GetMapping("/crane-utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'OPERATIONS')")
    public ResponseEntity<List<CraneUtilizationReportResponse>> getCraneUtilizationReport() {

        return ResponseEntity.ok(reportService.getCraneUtilizationReport());
    }

    // ------------- END CRANE-UTILIZATION REPORTS ---------------------



    // ------------- START REVENUE SUMMARY REPORTS ---------------------

    // Get revenue-summary
    @GetMapping("/revenue-summary")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary() {

        return ResponseEntity.ok(
                reportService.getRevenueSummary()
        );
    }

    // Get revenue-summary by date
    @GetMapping("/revenue-summary/date")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'FINANCE')")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummaryByDate(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {

        return ResponseEntity.ok(
                reportService.getRevenueSummaryByDate(
                        fromDate,
                        toDate
                )
        );
    }

    // Get monthly revenue
    @GetMapping("/revenue/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'FINANCE')")
    public ResponseEntity<List<MonthlyRevenueReportResponse>> getMonthlyRevenueReport() {

        return ResponseEntity.ok(
                reportService.getMonthlyRevenueReport());
    }

    // Get revenue by customer
    @GetMapping("/revenue/customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'FINANCE')")
    public ResponseEntity<List<CustomerRevenueReportResponse>> getCustomerRevenueReport() {

        return ResponseEntity.ok(
                reportService.getCustomerRevenueReport()
        );
    }

    // Get revenue by crane
    @GetMapping("/revenue/crane")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'FINANCE')")
    public ResponseEntity<List<CraneRevenueReportResponse>> getCraneRevenueReport() {

        return ResponseEntity.ok(
                reportService.getCraneRevenueReport()
        );
    }

    // ------------- END REVENUE SUMMARY REPORTS ---------------------



    // ------------- START MAINTENANCE REPORTS -----------------------

    // Get maintenance report
    @GetMapping("/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<List<MaintenanceReportResponse>> getMaintenanceReport() {

        return ResponseEntity.ok(
                reportService.getMaintenanceReport()
        );
    }

    // Get maintenance summary report
    @GetMapping("/maintenance/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<List<CraneMaintenanceSummaryResponse>> getCraneMaintenanceSummary() {

        return ResponseEntity.ok(
                reportService.getCraneMaintenanceSummary()
        );
    }

    // Get maintenance report by status
    @GetMapping("/maintenance/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'MAINTENANCE')")
    public ResponseEntity<List<MaintenanceStatusReportResponse>> getMaintenanceStatusReport() {

        return ResponseEntity.ok(
                reportService.getMaintenanceStatusReport()
        );
    }

    // ------------- END MAINTENANCE REPORTS -------------------------

}
