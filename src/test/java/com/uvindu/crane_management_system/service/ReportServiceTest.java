package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.*;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.InvoiceRepository;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import com.uvindu.crane_management_system.repository.MaintenanceRepository;
import com.uvindu.crane_management_system.repository.TimesheetRepository;
import com.uvindu.crane_management_system.util.ReportTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportServiceTest extends BaseUnitTest {

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private ReportService reportService;


    // =========================================================
    // JOB CARD REPORTS
    // =========================================================

    @Test
    void shouldGetAllJobCardReports() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        Operator operator =
                ReportTestDataBuilder.createOperator(
                        1L,
                        "Operator One"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        operator,
                        JobCardStatus.COMPLETED
                );

        when(jobCardRepository.findAll())
                .thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReport(null, null);

        // Assert
        assertThat(responses).hasSize(1);

        JobCardReportResponse response = responses.get(0);

        assertThat(response.getJobCardNumber())
                .isEqualTo("JC-0001");

        assertThat(response.getCustomerName())
                .isEqualTo("ABC Construction");

        assertThat(response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(response.getSalesPerson())
                .isEqualTo("sales01");

        assertThat(response.getOperatorName())
                .isEqualTo("Operator One");

        assertThat(response.getStatus())
                .isEqualTo("COMPLETED");

        verify(jobCardRepository).findAll();
    }


    @Test
    void shouldGetJobCardReportsByDateRange() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        LocalDate fromDate =
                LocalDate.of(2026, 9, 1);

        LocalDate toDate =
                LocalDate.of(2026, 9, 30);

        when(jobCardRepository.findByJobDateBetween(
                fromDate,
                toDate
        )).thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReport(
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getJobCardNumber())
                .isEqualTo("JC-0001");

        verify(jobCardRepository)
                .findByJobDateBetween(fromDate, toDate);

        verify(jobCardRepository, never()).findAll();
    }


    @Test
    void shouldGetJobCardReportsByCustomer() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        when(jobCardRepository.findByCustomerId_Id(1L))
                .thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReportByCustomer(1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerName())
                .isEqualTo("ABC Construction");

        verify(jobCardRepository)
                .findByCustomerId_Id(1L);
    }


    @Test
    void shouldGetJobCardReportsByCrane() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        when(jobCardRepository.findByCraneId_Id(1L))
                .thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReportByCrane(1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCraneNumber())
                .isEqualTo("CR012T01");

        verify(jobCardRepository)
                .findByCraneId_Id(1L);
    }


    @Test
    void shouldGetJobCardReportsByStatus() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.PENDING
                );

        when(jobCardRepository.findByStatus(JobCardStatus.PENDING))
                .thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReportByStatus(
                        JobCardStatus.PENDING
                );

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus())
                .isEqualTo("PENDING");

        verify(jobCardRepository)
                .findByStatus(JobCardStatus.PENDING);
    }


    @Test
    void shouldGetJobCardReportsBySalesPerson() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        when(jobCardRepository.findBySalesPersonId_Id(1L))
                .thenReturn(List.of(jobCard));

        // Act
        List<JobCardReportResponse> responses =
                reportService.getJobCardReportBySalesPerson(1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSalesPerson())
                .isEqualTo("sales01");

        verify(jobCardRepository)
                .findBySalesPersonId_Id(1L);
    }


// =========================================================
// INVOICE REPORTS
// =========================================================

    @Test
    void shouldGetAllInvoiceReports() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        when(invoiceRepository.findAll())
                .thenReturn(List.of(invoice));

        // Act
        List<InvoiceReportResponse> responses =
                reportService.getInvoiceReport();

        // Assert
        assertThat(responses).hasSize(1);

        InvoiceReportResponse response = responses.get(0);

        assertThat(response.getInvoiceNumber())
                .isEqualTo("INV-0001");

        assertThat(response.getCustomerName())
                .isEqualTo("ABC Construction");

        assertThat(response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(response.getJobCardNumber())
                .isEqualTo("JC-0001");

        assertThat(response.getInvoiceDate())
                .isEqualTo(LocalDate.of(2026, 9, 25));

        assertThat(response.getQty())
                .isEqualByComparingTo("8");

        assertThat(response.getRate())
                .isEqualByComparingTo("25000");

        assertThat(response.getAmount())
                .isEqualByComparingTo("200000");

        assertThat(response.getMobilization())
                .isEqualByComparingTo("15000");

        assertThat(response.getDescription())
                .isEqualTo("Test invoice");

        assertThat(response.getStatus())
                .isEqualTo("COMPLETED");

        verify(invoiceRepository).findAll();
    }


    @Test
    void shouldGetInvoiceReportsByDate() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        LocalDate fromDate =
                LocalDate.of(2026, 9, 1);

        LocalDate toDate =
                LocalDate.of(2026, 9, 30);

        Invoice invoice =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        when(invoiceRepository.findByInvoiceDateBetween(
                fromDate,
                toDate
        )).thenReturn(List.of(invoice));

        // Act
        List<InvoiceReportResponse> responses =
                reportService.getInvoiceReportByDate(
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(responses).hasSize(1);

        assertThat(responses.get(0).getInvoiceNumber())
                .isEqualTo("INV-0001");

        verify(invoiceRepository)
                .findByInvoiceDateBetween(fromDate, toDate);
    }


    @Test
    void shouldGetInvoiceReportsByCustomer() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        when(invoiceRepository.findByCustomer_Id(1L))
                .thenReturn(List.of(invoice));

        // Act
        List<InvoiceReportResponse> responses =
                reportService.getInvoiceReportByCustomer(1L);

        // Assert
        assertThat(responses).hasSize(1);

        assertThat(responses.get(0).getCustomerName())
                .isEqualTo("ABC Construction");

        verify(invoiceRepository)
                .findByCustomer_Id(1L);
    }


    @Test
    void shouldGetInvoiceReportsByStatus() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.CANCELED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        when(invoiceRepository.findByStatus(InvoiceStatus.CANCELED))
                .thenReturn(List.of(invoice));

        // Act
        List<InvoiceReportResponse> responses =
                reportService.getInvoiceReportByStatus(
                        InvoiceStatus.CANCELED
                );

        // Assert
        assertThat(responses).hasSize(1);

        assertThat(responses.get(0).getInvoiceNumber())
                .isEqualTo("INV-0001");

        assertThat(responses.get(0).getStatus())
                .isEqualTo("CANCELED");

        verify(invoiceRepository)
                .findByStatus(InvoiceStatus.CANCELED);
    }


    // =========================================================
    // CRANE UTILIZATION REPORTS
    // =========================================================

    @Test
    void shouldCalculateCraneUtilizationReport() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard1 =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        JobCard jobCard2 =
                ReportTestDataBuilder.createJobCard(
                        2L,
                        "JC-0002",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        /*
         * Two timesheets belong to the first job card.
         * One timesheet belongs to the second job card.
         *
         * Worked hours:
         * JC-0001 = 8 + 6 = 14
         * JC-0002 = 10
         *
         * Total = 24 hours
         *
         * Unique jobs = 2
         */

        Timesheet timesheet1 =
                ReportTestDataBuilder.createTimesheet(
                        1L,
                        jobCard1,
                        new BigDecimal("8")
                );

        Timesheet timesheet2 =
                ReportTestDataBuilder.createTimesheet(
                        2L,
                        jobCard1,
                        new BigDecimal("6")
                );

        Timesheet timesheet3 =
                ReportTestDataBuilder.createTimesheet(
                        3L,
                        jobCard2,
                        new BigDecimal("10")
                );

        when(timesheetRepository.findAllWithJobCardAndCrane())
                .thenReturn(
                        Arrays.asList(
                                timesheet1,
                                timesheet2,
                                timesheet3
                        )
                );

        // Act
        List<CraneUtilizationReportResponse> responses =
                reportService.getCraneUtilizationReport();

        // Assert
        assertThat(responses).hasSize(1);

        CraneUtilizationReportResponse response =
                responses.get(0);

        assertThat(response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(response.getCraneType())
                .isEqualTo("12T");

        assertThat(response.getTotalJobs())
                .isEqualTo(2);

        assertThat(response.getTotalWorkedHours())
                .isEqualByComparingTo("24");

        verify(timesheetRepository)
                .findAllWithJobCardAndCrane();
    }


    @Test
    void shouldReturnEmptyCraneUtilizationReportWhenNoTimesheetsExist() {

        // Arrange
        when(timesheetRepository.findAllWithJobCardAndCrane())
                .thenReturn(List.of());

        // Act
        List<CraneUtilizationReportResponse> responses =
                reportService.getCraneUtilizationReport();

        // Assert
        assertThat(responses).isEmpty();

        verify(timesheetRepository)
                .findAllWithJobCardAndCrane();
    }


    // =========================================================
    // REVENUE SUMMARY
    // =========================================================

    @Test
    void shouldCalculateRevenueSummary() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice1 =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 20),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        Invoice invoice2 =
                ReportTestDataBuilder.createInvoice(
                        2L,
                        "INV-0002",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("300000"),
                        new BigDecimal("20000")
                );

        when(invoiceRepository.findByStatus(InvoiceStatus.COMPLETED))
                .thenReturn(Arrays.asList(invoice1, invoice2));

        // Act
        RevenueSummaryResponse response =
                reportService.getRevenueSummary();

        // Assert
        assertThat(response.getTotalInvoices())
                .isEqualTo(2L);

        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("500000");

        assertThat(response.getTotalMobilization())
                .isEqualByComparingTo("35000");

        assertThat(response.getTotalRevenue())
                .isEqualByComparingTo("535000");

        verify(invoiceRepository)
                .findByStatus(InvoiceStatus.COMPLETED);
    }


    @Test
    void shouldCalculateRevenueSummaryByDate() {

        // Arrange
        LocalDate fromDate =
                LocalDate.of(2026, 9, 1);

        LocalDate toDate =
                LocalDate.of(2026, 9, 30);

        when(invoiceRepository.findByStatusAndInvoiceDateBetween(
                InvoiceStatus.COMPLETED,
                fromDate,
                toDate
        )).thenReturn(List.of());

        // Act
        RevenueSummaryResponse response =
                reportService.getRevenueSummaryByDate(
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(response.getTotalInvoices())
                .isEqualTo(0L);

        assertThat(response.getTotalAmount())
                .isEqualByComparingTo("0");

        assertThat(response.getTotalMobilization())
                .isEqualByComparingTo("0");

        assertThat(response.getTotalRevenue())
                .isEqualByComparingTo("0");

        verify(invoiceRepository)
                .findByStatusAndInvoiceDateBetween(
                        InvoiceStatus.COMPLETED,
                        fromDate,
                        toDate
                );
    }


    @Test
    void shouldCalculateMonthlyRevenueReport() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice1 =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 10),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        Invoice invoice2 =
                ReportTestDataBuilder.createInvoice(
                        2L,
                        "INV-0002",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 20),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("300000"),
                        new BigDecimal("20000")
                );

        Invoice invoice3 =
                ReportTestDataBuilder.createInvoice(
                        3L,
                        "INV-0003",
                        customer,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 10, 5),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("100000"),
                        new BigDecimal("10000")
                );

        when(invoiceRepository.findByStatus(InvoiceStatus.COMPLETED))
                .thenReturn(
                        Arrays.asList(
                                invoice1,
                                invoice2,
                                invoice3
                        )
                );

        // Act
        List<MonthlyRevenueReportResponse> responses =
                reportService.getMonthlyRevenueReport();

        // Assert
        assertThat(responses).hasSize(2);


        // September 2026
        MonthlyRevenueReportResponse september =
                responses.get(0);

        assertThat(september.getYear())
                .isEqualTo(2026);

        assertThat(september.getMonth())
                .isEqualTo(9);

        assertThat(september.getMonthName())
                .isEqualTo("September");

        assertThat(september.getTotalInvoices())
                .isEqualTo(2L);

        assertThat(september.getTotalAmount())
                .isEqualByComparingTo("500000");

        assertThat(september.getTotalMobilization())
                .isEqualByComparingTo("35000");

        assertThat(september.getTotalRevenue())
                .isEqualByComparingTo("535000");


        // October 2026
        MonthlyRevenueReportResponse october =
                responses.get(1);

        assertThat(october.getYear())
                .isEqualTo(2026);

        assertThat(october.getMonth())
                .isEqualTo(10);

        assertThat(october.getMonthName())
                .isEqualTo("October");

        assertThat(october.getTotalInvoices())
                .isEqualTo(1L);

        assertThat(october.getTotalAmount())
                .isEqualByComparingTo("100000");

        assertThat(october.getTotalMobilization())
                .isEqualByComparingTo("10000");

        assertThat(october.getTotalRevenue())
                .isEqualByComparingTo("110000");

        verify(invoiceRepository)
                .findByStatus(InvoiceStatus.COMPLETED);
    }

    @Test
    void shouldCalculateCustomerRevenueReport() {

        // Arrange
        Customer customer1 =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Customer customer2 =
                ReportTestDataBuilder.createCustomer(
                        2L,
                        "XYZ Engineering"
                );

        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer1,
                        crane,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice1 =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer1,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 10),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        Invoice invoice2 =
                ReportTestDataBuilder.createInvoice(
                        2L,
                        "INV-0002",
                        customer1,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 20),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("300000"),
                        new BigDecimal("20000")
                );

        Invoice invoice3 =
                ReportTestDataBuilder.createInvoice(
                        3L,
                        "INV-0003",
                        customer2,
                        crane,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("100000"),
                        new BigDecimal("10000")
                );

        when(invoiceRepository.findByStatus(InvoiceStatus.COMPLETED))
                .thenReturn(
                        Arrays.asList(
                                invoice1,
                                invoice2,
                                invoice3
                        )
                );

        // Act
        List<CustomerRevenueReportResponse> responses =
                reportService.getCustomerRevenueReport();

        // Assert
        assertThat(responses).hasSize(2);


        // Customer 1
        CustomerRevenueReportResponse customer1Response =
                responses.get(0);

        assertThat(customer1Response.getCustomerId())
                .isEqualTo(1L);

        assertThat(customer1Response.getCustomerName())
                .isEqualTo("ABC Construction");

        assertThat(customer1Response.getTotalInvoices())
                .isEqualTo(2L);

        assertThat(customer1Response.getTotalAmount())
                .isEqualByComparingTo("500000");

        assertThat(customer1Response.getTotalMobilization())
                .isEqualByComparingTo("35000");

        assertThat(customer1Response.getTotalRevenue())
                .isEqualByComparingTo("535000");


        // Customer 2
        CustomerRevenueReportResponse customer2Response =
                responses.get(1);

        assertThat(customer2Response.getCustomerId())
                .isEqualTo(2L);

        assertThat(customer2Response.getCustomerName())
                .isEqualTo("XYZ Engineering");

        assertThat(customer2Response.getTotalInvoices())
                .isEqualTo(1L);

        assertThat(customer2Response.getTotalAmount())
                .isEqualByComparingTo("100000");

        assertThat(customer2Response.getTotalMobilization())
                .isEqualByComparingTo("10000");

        assertThat(customer2Response.getTotalRevenue())
                .isEqualByComparingTo("110000");

        verify(invoiceRepository)
                .findByStatus(InvoiceStatus.COMPLETED);
    }


    @Test
    void shouldCalculateCraneRevenueReport() {

        // Arrange
        Customer customer =
                ReportTestDataBuilder.createCustomer(
                        1L,
                        "ABC Construction"
                );

        Crane crane1 =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        Crane crane2 =
                ReportTestDataBuilder.createCrane(
                        2L,
                        "CR120T01",
                        "120T"
                );

        User salesPerson =
                ReportTestDataBuilder.createSalesPerson(
                        1L,
                        "sales01"
                );

        JobCard jobCard =
                ReportTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        customer,
                        crane1,
                        salesPerson,
                        null,
                        JobCardStatus.COMPLETED
                );

        Invoice invoice1 =
                ReportTestDataBuilder.createInvoice(
                        1L,
                        "INV-0001",
                        customer,
                        crane1,
                        jobCard,
                        LocalDate.of(2026, 9, 10),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("200000"),
                        new BigDecimal("15000")
                );

        Invoice invoice2 =
                ReportTestDataBuilder.createInvoice(
                        2L,
                        "INV-0002",
                        customer,
                        crane1,
                        jobCard,
                        LocalDate.of(2026, 9, 20),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("300000"),
                        new BigDecimal("20000")
                );

        Invoice invoice3 =
                ReportTestDataBuilder.createInvoice(
                        3L,
                        "INV-0003",
                        customer,
                        crane2,
                        jobCard,
                        LocalDate.of(2026, 9, 25),
                        InvoiceStatus.COMPLETED,
                        new BigDecimal("100000"),
                        new BigDecimal("10000")
                );

        when(invoiceRepository.findByStatus(InvoiceStatus.COMPLETED))
                .thenReturn(
                        Arrays.asList(
                                invoice1,
                                invoice2,
                                invoice3
                        )
                );

        // Act
        List<CraneRevenueReportResponse> responses =
                reportService.getCraneRevenueReport();

        // Assert
        assertThat(responses).hasSize(2);


        // Crane 1
        CraneRevenueReportResponse crane1Response =
                responses.get(0);

        assertThat(crane1Response.getCraneId())
                .isEqualTo(1L);

        assertThat(crane1Response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(crane1Response.getCraneType())
                .isEqualTo("12T");

        assertThat(crane1Response.getTotalInvoices())
                .isEqualTo(2L);

        assertThat(crane1Response.getTotalAmount())
                .isEqualByComparingTo("500000");

        assertThat(crane1Response.getTotalMobilization())
                .isEqualByComparingTo("35000");

        assertThat(crane1Response.getTotalRevenue())
                .isEqualByComparingTo("535000");


        // Crane 2
        CraneRevenueReportResponse crane2Response =
                responses.get(1);

        assertThat(crane2Response.getCraneId())
                .isEqualTo(2L);

        assertThat(crane2Response.getCraneNumber())
                .isEqualTo("CR120T01");

        assertThat(crane2Response.getCraneType())
                .isEqualTo("120T");

        assertThat(crane2Response.getTotalInvoices())
                .isEqualTo(1L);

        assertThat(crane2Response.getTotalAmount())
                .isEqualByComparingTo("100000");

        assertThat(crane2Response.getTotalMobilization())
                .isEqualByComparingTo("10000");

        assertThat(crane2Response.getTotalRevenue())
                .isEqualByComparingTo("110000");

        verify(invoiceRepository)
                .findByStatus(InvoiceStatus.COMPLETED);
    }


    // =========================================================
// MAINTENANCE REPORTS
// =========================================================

    @Test
    void shouldGetMaintenanceReport() {

        // Arrange
        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User maintenancePerson =
                ReportTestDataBuilder.createMaintenancePerson(
                        1L,
                        "maintenance01"
                );

        Maintenance maintenance =
                ReportTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        crane,
                        maintenancePerson,
                        MaintenanceType.PREVENTIVE,
                        MaintenanceStatus.COMPLETED,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12)
                );

        when(maintenanceRepository.findAll())
                .thenReturn(List.of(maintenance));

        // Act
        List<MaintenanceReportResponse> responses =
                reportService.getMaintenanceReport();

        // Assert
        assertThat(responses).hasSize(1);

        MaintenanceReportResponse response =
                responses.get(0);

        assertThat(response.getMaintenanceNumber())
                .isEqualTo("MT-0001");

        assertThat(response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(response.getCraneType())
                .isEqualTo("12T");

        assertThat(response.getMaintenanceDate())
                .isEqualTo(LocalDate.of(2026, 9, 20));

        assertThat(response.getStartDate())
                .isEqualTo(LocalDate.of(2026, 9, 10));

        assertThat(response.getEndDate())
                .isEqualTo(LocalDate.of(2026, 9, 12));

        assertThat(response.getRepairType())
                .isEqualTo("PREVENTIVE");

        assertThat(response.getMaintenancePerson())
                .isEqualTo("maintenance01");

        assertThat(response.getStatus())
                .isEqualTo("COMPLETED");

        verify(maintenanceRepository).findAll();
    }


    @Test
    void shouldCalculateCraneMaintenanceSummary() {

        // Arrange
        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User maintenancePerson =
                ReportTestDataBuilder.createMaintenancePerson(
                        1L,
                        "maintenance01"
                );

        Maintenance maintenance1 =
                ReportTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        crane,
                        maintenancePerson,
                        MaintenanceType.PREVENTIVE,
                        MaintenanceStatus.COMPLETED,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12)
                );

        Maintenance maintenance2 =
                ReportTestDataBuilder.createMaintenance(
                        2L,
                        "MT-0002",
                        crane,
                        maintenancePerson,
                        MaintenanceType.SERVICE,
                        MaintenanceStatus.COMPLETED,
                        LocalDate.of(2026, 9, 20),
                        LocalDate.of(2026, 9, 21)
                );

        when(maintenanceRepository.findAll())
                .thenReturn(
                        Arrays.asList(
                                maintenance1,
                                maintenance2
                        )
                );

        // Act
        List<CraneMaintenanceSummaryResponse> responses =
                reportService.getCraneMaintenanceSummary();

        // Assert
        assertThat(responses).hasSize(1);

        CraneMaintenanceSummaryResponse response =
                responses.get(0);

        assertThat(response.getCraneId())
                .isEqualTo(1L);

        assertThat(response.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(response.getCraneType())
                .isEqualTo("12T");

        assertThat(response.getTotalMaintenanceRecords())
                .isEqualTo(2L);

        /*
         * MT-0001 = 10, 11, 12 = 3 days
         * MT-0002 = 20, 21 = 2 days
         *
         * Total = 5 days
         */
        assertThat(response.getTotalMaintenanceDays())
                .isEqualTo(5L);

        verify(maintenanceRepository).findAll();
    }


    @Test
    void shouldIncludeMaintenanceRecordButNotCountDaysWhenDatesAreNull() {

        // Arrange
        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User maintenancePerson =
                ReportTestDataBuilder.createMaintenancePerson(
                        1L,
                        "maintenance01"
                );

        Maintenance maintenance =
                ReportTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        crane,
                        maintenancePerson,
                        MaintenanceType.SERVICE,
                        MaintenanceStatus.SCHEDULED,
                        null,
                        null
                );

        when(maintenanceRepository.findAll())
                .thenReturn(List.of(maintenance));

        // Act
        List<CraneMaintenanceSummaryResponse> responses =
                reportService.getCraneMaintenanceSummary();

        // Assert
        assertThat(responses).hasSize(1);

        CraneMaintenanceSummaryResponse response =
                responses.get(0);

        assertThat(response.getTotalMaintenanceRecords())
                .isEqualTo(1L);

        assertThat(response.getTotalMaintenanceDays())
                .isEqualTo(0L);

        verify(maintenanceRepository).findAll();
    }

    @Test
    void shouldCalculateMaintenanceStatusReport() {

        // Arrange
        Crane crane =
                ReportTestDataBuilder.createCrane(
                        1L,
                        "CR012T01",
                        "12T"
                );

        User maintenancePerson =
                ReportTestDataBuilder.createMaintenancePerson(
                        1L,
                        "maintenance01"
                );

        Maintenance scheduled =
                ReportTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        crane,
                        maintenancePerson,
                        MaintenanceType.PREVENTIVE,
                        MaintenanceStatus.SCHEDULED,
                        null,
                        null
                );

        Maintenance inProgress =
                ReportTestDataBuilder.createMaintenance(
                        2L,
                        "MT-0002",
                        crane,
                        maintenancePerson,
                        MaintenanceType.SERVICE,
                        MaintenanceStatus.IN_PROGRESS,
                        null,
                        null
                );

        Maintenance completed =
                ReportTestDataBuilder.createMaintenance(
                        3L,
                        "MT-0003",
                        crane,
                        maintenancePerson,
                        MaintenanceType.CORRECTIVE,
                        MaintenanceStatus.COMPLETED,
                        null,
                        null
                );

        Maintenance canceled =
                ReportTestDataBuilder.createMaintenance(
                        4L,
                        "MT-0004",
                        crane,
                        maintenancePerson,
                        MaintenanceType.SERVICE,
                        MaintenanceStatus.CANCELED,
                        null,
                        null
                );

        Maintenance completed2 =
                ReportTestDataBuilder.createMaintenance(
                        5L,
                        "MT-0005",
                        crane,
                        maintenancePerson,
                        MaintenanceType.PREVENTIVE,
                        MaintenanceStatus.COMPLETED,
                        null,
                        null
                );

        when(maintenanceRepository.findAll())
                .thenReturn(
                        Arrays.asList(
                                scheduled,
                                inProgress,
                                completed,
                                canceled,
                                completed2
                        )
                );

        // Act
        List<MaintenanceStatusReportResponse> responses =
                reportService.getMaintenanceStatusReport();

        // Assert
        assertThat(responses).hasSize(
                MaintenanceStatus.values().length
        );

        MaintenanceStatusReportResponse scheduledResponse =
                responses.stream()
                        .filter(response ->
                                response.getStatus()
                                        .equals("SCHEDULED"))
                        .findFirst()
                        .orElseThrow();

        MaintenanceStatusReportResponse inProgressResponse =
                responses.stream()
                        .filter(response ->
                                response.getStatus()
                                        .equals("IN_PROGRESS"))
                        .findFirst()
                        .orElseThrow();

        MaintenanceStatusReportResponse completedResponse =
                responses.stream()
                        .filter(response ->
                                response.getStatus()
                                        .equals("COMPLETED"))
                        .findFirst()
                        .orElseThrow();

        MaintenanceStatusReportResponse canceledResponse =
                responses.stream()
                        .filter(response ->
                                response.getStatus()
                                        .equals("CANCELED"))
                        .findFirst()
                        .orElseThrow();

        assertThat(scheduledResponse.getTotal())
                .isEqualTo(1L);

        assertThat(inProgressResponse.getTotal())
                .isEqualTo(1L);

        assertThat(completedResponse.getTotal())
                .isEqualTo(2L);

        assertThat(canceledResponse.getTotal())
                .isEqualTo(1L);

        verify(maintenanceRepository).findAll();
    }

}