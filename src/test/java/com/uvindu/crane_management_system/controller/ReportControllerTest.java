package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    // ============================================================
    // JOB-CARD REPORTS
    // ============================================================

    @Test
    void shouldGetJobCardReport() {

        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        List<JobCardReportResponse> report =
                List.of(new JobCardReportResponse());

        when(reportService.getJobCardReport(fromDate, toDate))
                .thenReturn(report);

        ResponseEntity<List<JobCardReportResponse>> response =
                reportController.getJobCardReport(fromDate, toDate);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getJobCardReport(fromDate, toDate);
    }

    @Test
    void shouldGetJobCardReportByCustomer() {

        Long customerId = 1L;

        List<JobCardReportResponse> report =
                List.of(new JobCardReportResponse());

        when(reportService.getJobCardReportByCustomer(customerId))
                .thenReturn(report);

        ResponseEntity<List<JobCardReportResponse>> response =
                reportController.getJobCardReportByCustomer(customerId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getJobCardReportByCustomer(customerId);
    }

    @Test
    void shouldGetJobCardReportByCrane() {

        Long craneId = 1L;

        List<JobCardReportResponse> report =
                List.of(new JobCardReportResponse());

        when(reportService.getJobCardReportByCrane(craneId))
                .thenReturn(report);

        ResponseEntity<List<JobCardReportResponse>> response =
                reportController.getJobCardReportByCrane(craneId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getJobCardReportByCrane(craneId);
    }

    @Test
    void shouldGetJobCardReportByStatus() {

        JobCardStatus status = JobCardStatus.COMPLETED;

        List<JobCardReportResponse> report =
                List.of(new JobCardReportResponse());

        when(reportService.getJobCardReportByStatus(status))
                .thenReturn(report);

        ResponseEntity<List<JobCardReportResponse>> response =
                reportController.getJobCardReportByStatus(status);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getJobCardReportByStatus(status);
    }

    @Test
    void shouldGetJobCardReportBySalesPerson() {

        Long salesPersonId = 1L;

        List<JobCardReportResponse> report =
                List.of(new JobCardReportResponse());

        when(reportService.getJobCardReportBySalesPerson(salesPersonId))
                .thenReturn(report);

        ResponseEntity<List<JobCardReportResponse>> response =
                reportController.getJobCardReportBySalesPerson(salesPersonId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getJobCardReportBySalesPerson(salesPersonId);
    }

    // ============================================================
    // INVOICE REPORTS
    // ============================================================

    @Test
    void shouldGetInvoiceReport() {

        List<InvoiceReportResponse> report =
                List.of(new InvoiceReportResponse());

        when(reportService.getInvoiceReport())
                .thenReturn(report);

        ResponseEntity<List<InvoiceReportResponse>> response =
                reportController.getInvoiceReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getInvoiceReport();
    }

    @Test
    void shouldGetInvoiceReportByDate() {

        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        List<InvoiceReportResponse> report =
                List.of(new InvoiceReportResponse());

        when(reportService.getInvoiceReportByDate(fromDate, toDate))
                .thenReturn(report);

        ResponseEntity<List<InvoiceReportResponse>> response =
                reportController.getInvoiceReportByDate(fromDate, toDate);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getInvoiceReportByDate(fromDate, toDate);
    }

    @Test
    void shouldGetInvoiceReportByCustomer() {

        Long customerId = 1L;

        List<InvoiceReportResponse> report =
                List.of(new InvoiceReportResponse());

        when(reportService.getInvoiceReportByCustomer(customerId))
                .thenReturn(report);

        ResponseEntity<List<InvoiceReportResponse>> response =
                reportController.getInvoiceReportByCustomer(customerId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getInvoiceReportByCustomer(customerId);
    }

    @Test
    void shouldGetInvoiceReportByStatus() {

        InvoiceStatus status = InvoiceStatus.DRAFT;

        List<InvoiceReportResponse> report =
                List.of(new InvoiceReportResponse());

        when(reportService.getInvoiceReportByStatus(status))
                .thenReturn(report);

        ResponseEntity<List<InvoiceReportResponse>> response =
                reportController.getInvoiceReportByStatus(status);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getInvoiceReportByStatus(status);
    }

    // ============================================================
    // CRANE UTILIZATION REPORTS
    // ============================================================

    @Test
    void shouldGetCraneUtilizationReport() {

        List<CraneUtilizationReportResponse> report =
                List.of(new CraneUtilizationReportResponse());

        when(reportService.getCraneUtilizationReport())
                .thenReturn(report);

        ResponseEntity<List<CraneUtilizationReportResponse>> response =
                reportController.getCraneUtilizationReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getCraneUtilizationReport();
    }

    // ============================================================
    // REVENUE SUMMARY REPORTS
    // ============================================================

    @Test
    void shouldGetRevenueSummary() {

        RevenueSummaryResponse report =
                new RevenueSummaryResponse();

        when(reportService.getRevenueSummary())
                .thenReturn(report);

        ResponseEntity<RevenueSummaryResponse> response =
                reportController.getRevenueSummary();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getRevenueSummary();
    }

    @Test
    void shouldGetRevenueSummaryByDate() {

        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        RevenueSummaryResponse report =
                new RevenueSummaryResponse();

        when(reportService.getRevenueSummaryByDate(fromDate, toDate))
                .thenReturn(report);

        ResponseEntity<RevenueSummaryResponse> response =
                reportController.getRevenueSummaryByDate(fromDate, toDate);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getRevenueSummaryByDate(fromDate, toDate);
    }

    @Test
    void shouldGetMonthlyRevenueReport() {

        List<MonthlyRevenueReportResponse> report =
                List.of(new MonthlyRevenueReportResponse());

        when(reportService.getMonthlyRevenueReport())
                .thenReturn(report);

        ResponseEntity<List<MonthlyRevenueReportResponse>> response =
                reportController.getMonthlyRevenueReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getMonthlyRevenueReport();
    }

    @Test
    void shouldGetCustomerRevenueReport() {

        List<CustomerRevenueReportResponse> report =
                List.of(new CustomerRevenueReportResponse());

        when(reportService.getCustomerRevenueReport())
                .thenReturn(report);

        ResponseEntity<List<CustomerRevenueReportResponse>> response =
                reportController.getCustomerRevenueReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getCustomerRevenueReport();
    }

    @Test
    void shouldGetCraneRevenueReport() {

        List<CraneRevenueReportResponse> report =
                List.of(new CraneRevenueReportResponse());

        when(reportService.getCraneRevenueReport())
                .thenReturn(report);

        ResponseEntity<List<CraneRevenueReportResponse>> response =
                reportController.getCraneRevenueReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getCraneRevenueReport();
    }

    // ============================================================
    // MAINTENANCE REPORTS
    // ============================================================

    @Test
    void shouldGetMaintenanceReport() {

        List<MaintenanceReportResponse> report =
                List.of(new MaintenanceReportResponse());

        when(reportService.getMaintenanceReport())
                .thenReturn(report);

        ResponseEntity<List<MaintenanceReportResponse>> response =
                reportController.getMaintenanceReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getMaintenanceReport();
    }

    @Test
    void shouldGetCraneMaintenanceSummary() {

        List<CraneMaintenanceSummaryResponse> report =
                List.of(new CraneMaintenanceSummaryResponse());

        when(reportService.getCraneMaintenanceSummary())
                .thenReturn(report);

        ResponseEntity<List<CraneMaintenanceSummaryResponse>> response =
                reportController.getCraneMaintenanceSummary();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getCraneMaintenanceSummary();
    }

    @Test
    void shouldGetMaintenanceStatusReport() {

        List<MaintenanceStatusReportResponse> report =
                List.of(new MaintenanceStatusReportResponse());

        when(reportService.getMaintenanceStatusReport())
                .thenReturn(report);

        ResponseEntity<List<MaintenanceStatusReportResponse>> response =
                reportController.getMaintenanceStatusReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(report);

        verify(reportService).getMaintenanceStatusReport();
    }
}