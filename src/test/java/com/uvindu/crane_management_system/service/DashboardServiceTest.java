package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CraneIdleResponse;
import com.uvindu.crane_management_system.dto.DashboardResponse;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DashboardServiceTest extends BaseUnitTest {

    @Mock
    private CraneRepository craneRepository;

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private TimesheetRepository timesheetRepository;

    @InjectMocks
    private DashboardService dashboardService;


    @Test
    void shouldGetDashboardSummarySuccessfully() {
        // Arrange

        // Crane summary
        when(craneRepository.count()).thenReturn(10L);
        when(craneRepository.countByStatus(CraneStatus.AVAILABLE))
                .thenReturn(5L);
        when(craneRepository.countByStatus(CraneStatus.IN_USE))
                .thenReturn(3L);
        when(craneRepository.countByStatus(CraneStatus.MAINTENANCE))
                .thenReturn(2L);

        // Job summary
        when(jobCardRepository.countByStatus(JobCardStatus.PENDING))
                .thenReturn(4L);
        when(jobCardRepository.countByStatus(JobCardStatus.COMPLETED))
                .thenReturn(8L);
        when(jobCardRepository.countByStatus(JobCardStatus.CANCELED))
                .thenReturn(2L);

        // Invoice summary
        when(invoiceRepository.count()).thenReturn(12L);
        when(invoiceRepository.countByStatus(InvoiceStatus.DRAFT))
                .thenReturn(3L);
        when(invoiceRepository.countByStatus(InvoiceStatus.COMPLETED))
                .thenReturn(6L);
        when(invoiceRepository.countByStatus(InvoiceStatus.REVISED))
                .thenReturn(2L);
        when(invoiceRepository.countByStatus(InvoiceStatus.CANCELED))
                .thenReturn(1L);

        // Act
        DashboardResponse result = dashboardService.getDashboard();

        // Assert
        assertThat(result).isNotNull();

        // Crane summary
        assertThat(result.getTotalCranes()).isEqualTo(10L);
        assertThat(result.getAvailableCranes()).isEqualTo(5L);
        assertThat(result.getInUseCranes()).isEqualTo(3L);
        assertThat(result.getMaintenanceCranes()).isEqualTo(2L);

        // Job summary
        assertThat(result.getPendingJobs()).isEqualTo(4L);
        assertThat(result.getCompletedJobs()).isEqualTo(8L);
        assertThat(result.getCanceledJobs()).isEqualTo(2L);

        // Invoice summary
        assertThat(result.getTotalInvoices()).isEqualTo(12L);
        assertThat(result.getDraftInvoices()).isEqualTo(3L);
        assertThat(result.getCompletedInvoices()).isEqualTo(6L);
        assertThat(result.getRevisedInvoices()).isEqualTo(2L);
        assertThat(result.getCanceledInvoices()).isEqualTo(1L);

        // Verify repository calls
        verify(craneRepository).count();
        verify(craneRepository).countByStatus(CraneStatus.AVAILABLE);
        verify(craneRepository).countByStatus(CraneStatus.IN_USE);
        verify(craneRepository).countByStatus(CraneStatus.MAINTENANCE);

        verify(jobCardRepository).countByStatus(JobCardStatus.PENDING);
        verify(jobCardRepository).countByStatus(JobCardStatus.COMPLETED);
        verify(jobCardRepository).countByStatus(JobCardStatus.CANCELED);

        verify(invoiceRepository).count();
        verify(invoiceRepository).countByStatus(InvoiceStatus.DRAFT);
        verify(invoiceRepository).countByStatus(InvoiceStatus.COMPLETED);
        verify(invoiceRepository).countByStatus(InvoiceStatus.REVISED);
        verify(invoiceRepository).countByStatus(InvoiceStatus.CANCELED);
    }


    @Test
    void shouldReturnZeroCountsWhenNoDataExists() {
        // Arrange

        // Mockito returns 0L for unstubbed long return values.
        // No additional stubbing is required.

        // Act
        DashboardResponse result = dashboardService.getDashboard();

        // Assert
        assertThat(result).isNotNull();

        assertThat(result.getTotalCranes()).isZero();
        assertThat(result.getAvailableCranes()).isZero();
        assertThat(result.getInUseCranes()).isZero();
        assertThat(result.getMaintenanceCranes()).isZero();

        assertThat(result.getPendingJobs()).isZero();
        assertThat(result.getCompletedJobs()).isZero();
        assertThat(result.getCanceledJobs()).isZero();

        assertThat(result.getTotalInvoices()).isZero();
        assertThat(result.getDraftInvoices()).isZero();
        assertThat(result.getCompletedInvoices()).isZero();
        assertThat(result.getRevisedInvoices()).isZero();
        assertThat(result.getCanceledInvoices()).isZero();
    }


    @Test
    void shouldCalculateAllDaysAsIdleWhenCraneHasNoActivity() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getCraneId()).isEqualTo(1L);
        assertThat(statistics.getCraneNumber())
                .isEqualTo("CR012T01");

        assertThat(statistics.getWorkedDays()).isZero();
        assertThat(statistics.getReservedDays()).isZero();
        assertThat(statistics.getMaintenanceDays()).isZero();
        assertThat(statistics.getIdleDays()).isEqualTo(10);
    }


    @Test
    void shouldExcludePendingReservationDaysFromIdleDays() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        JobCard reservation = createReservation(
                crane,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 6)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(reservation));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getReservedDays()).isEqualTo(4);
        assertThat(statistics.getIdleDays()).isEqualTo(6);
        assertThat(statistics.getWorkedDays()).isZero();
        assertThat(statistics.getMaintenanceDays()).isZero();

        verify(jobCardRepository).findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        );
    }


    @Test
    void shouldCalculateWorkedDaysFromTimesheets() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        Timesheet first = createTimesheet(
                crane, LocalDate.of(2026, 10, 2)
        );

        Timesheet second = createTimesheet(
                crane, LocalDate.of(2026, 10, 3)
        );

        Timesheet third = createTimesheet(
                crane, LocalDate.of(2026, 10, 4)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(timesheetRepository.findWorkWithinPeriod(
                JobCardStatus.COMPLETED, fromDate, toDate
        )).thenReturn(List.of(first, second, third));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getWorkedDays()).isEqualTo(3);
        assertThat(statistics.getReservedDays()).isZero();
        assertThat(statistics.getMaintenanceDays()).isZero();
        assertThat(statistics.getIdleDays()).isEqualTo(7);

        verify(timesheetRepository).findWorkWithinPeriod(
                JobCardStatus.COMPLETED, fromDate, toDate
        );
    }


    @Test
    void shouldExcludeMaintenanceDaysFromIdleDays() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        Maintenance maintenance = createMaintenance(
                crane,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(maintenanceRepository.findMaintenanceOverlapping(
                List.of(
                        MaintenanceStatus.SCHEDULED,
                        MaintenanceStatus.IN_PROGRESS,
                        MaintenanceStatus.COMPLETED
                ),
                fromDate,
                toDate
        )).thenReturn(List.of(maintenance));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getMaintenanceDays()).isEqualTo(3);
        assertThat(statistics.getIdleDays()).isEqualTo(7);
        assertThat(statistics.getWorkedDays()).isZero();
        assertThat(statistics.getReservedDays()).isZero();
    }


    @Test
    void shouldNotDoubleCountOverlappingReservations() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        JobCard first = createReservation(
                crane,
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5)
        );

        JobCard second = createReservation(
                crane,
                LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 7)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(first, second));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        // October 2 to October 7 = 6 unique dates
        assertThat(statistics.getReservedDays()).isEqualTo(6);
        assertThat(statistics.getIdleDays()).isEqualTo(4);
    }


    @Test
    void shouldPrioritizeMaintenanceOverWorkedAndReservedDays() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        JobCard reservation = createReservation(
                crane,
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 5)
        );

        Timesheet timesheet = createTimesheet(
                crane, LocalDate.of(2026, 10, 4)
        );

        Maintenance maintenance = createMaintenance(
                crane,
                LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 6)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(reservation));

        when(timesheetRepository.findWorkWithinPeriod(
                JobCardStatus.COMPLETED, fromDate, toDate
        )).thenReturn(List.of(timesheet));

        when(maintenanceRepository.findMaintenanceOverlapping(
                List.of(
                        MaintenanceStatus.SCHEDULED,
                        MaintenanceStatus.IN_PROGRESS,
                        MaintenanceStatus.COMPLETED
                ),
                fromDate,
                toDate
        )).thenReturn(List.of(maintenance));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getMaintenanceDays()).isEqualTo(3);
        assertThat(statistics.getReservedDays()).isEqualTo(2);
        assertThat(statistics.getWorkedDays()).isZero();
        assertThat(statistics.getIdleDays()).isEqualTo(5);

        assertThat(
                statistics.getMaintenanceDays()
                        + statistics.getWorkedDays()
                        + statistics.getReservedDays()
                        + statistics.getIdleDays()
        ).isEqualTo(10);
    }


    @Test
    void shouldClipReservationDatesToReportingPeriod() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        JobCard reservation = createReservation(
                crane,
                LocalDate.of(2026, 9, 28),
                LocalDate.of(2026, 10, 3)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(reservation));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getReservedDays()).isEqualTo(3);
        assertThat(statistics.getIdleDays()).isEqualTo(7);
    }

    @Test
    void shouldTreatNullReservationEndDateAsReportingEndDate() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane crane = createCrane(1L, "CR012T01");

        JobCard reservation = createReservation(
                crane,
                LocalDate.of(2026, 10, 6),
                null
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(crane));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(reservation));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        CraneIdleResponse statistics = result.getFirst();

        assertThat(statistics.getReservedDays()).isEqualTo(5);
        assertThat(statistics.getIdleDays()).isEqualTo(5);
    }


    @Test
    void shouldCalculateStatisticsSeparatelyForEachCraneAndSortByNumber() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        Crane craneB = createCrane(2L, "CR020T02");
        Crane craneA = createCrane(1L, "CR012T01");

        JobCard reservation = createReservation(
                craneB,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3)
        );

        when(craneRepository.findAll())
                .thenReturn(List.of(craneB, craneA));

        when(jobCardRepository.findReservationsOverlapping(
                JobCardStatus.PENDING, fromDate, toDate
        )).thenReturn(List.of(reservation));

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(CraneIdleResponse::getCraneNumber)
                .containsExactly("CR012T01", "CR020T02");

        assertThat(result.get(0).getIdleDays()).isEqualTo(10);

        assertThat(result.get(1).getReservedDays()).isEqualTo(3);
        assertThat(result.get(1).getIdleDays()).isEqualTo(7);
    }


    @Test
    void shouldReturnEmptyStatisticsWhenNoCranesExist() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 10);

        // Act
        List<CraneIdleResponse> result =
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                );

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 1);

        // Act & Assert
        assertThatThrownBy(() ->
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                )
        ).isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> {
                    assertThat(exception.getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST);

                    assertThat(exception.getReason())
                            .isEqualTo("Invalid reporting date range");
                }
        );

        verifyNoInteractions(
                craneRepository,
                jobCardRepository,
                timesheetRepository,
                maintenanceRepository
        );
    }


    @Test
    void shouldRejectNullReportingDates() {

        // Arrange
        LocalDate validDate = LocalDate.of(2026, 10, 10);

        // Act & Assert
        assertThatThrownBy(() ->
                dashboardService.getCraneIdleStatistics(
                        null, validDate
                )
        ).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() ->
                dashboardService.getCraneIdleStatistics(
                        validDate, null
                )
        ).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(
                craneRepository,
                jobCardRepository,
                timesheetRepository,
                maintenanceRepository
        );
    }


    @Test
    void shouldRejectFutureReportingEndDate() {

        // Arrange
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(1);

        // Act & Assert
        assertThatThrownBy(() ->
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                )
        ).isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> {
                    assertThat(exception.getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST);

                    assertThat(exception.getReason())
                            .isEqualTo(
                                    "Reporting end date cannot be in the future"
                            );
                }
        );
    }


    @Test
    void shouldRejectReportingPeriodLongerThanAllowed() {

        // Arrange
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(366);

        // Act & Assert
        assertThatThrownBy(() ->
                dashboardService.getCraneIdleStatistics(
                        fromDate, toDate
                )
        ).isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> {
                    assertThat(exception.getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST);

                    assertThat(exception.getReason())
                            .isEqualTo(
                                    "Reporting period cannot exceed 366 days"
                            );
                }
        );
    }




    private Crane createCrane(Long id, String craneNumber) {
        Crane crane = mock(Crane.class);

        when(crane.getId()).thenReturn(id);
        when(crane.getCraneNumber()).thenReturn(craneNumber);

        return crane;
    }

    private JobCard createReservation(
            Crane crane,
            LocalDate startDate,
            LocalDate endDate
    ) {
        JobCard jobCard = mock(JobCard.class);

        when(jobCard.getCraneId()).thenReturn(crane);
        when(jobCard.getStartDate()).thenReturn(startDate);

        if (endDate != null) {
            when(jobCard.getEndDate()).thenReturn(endDate);
        }

        return jobCard;
    }

    private Timesheet createTimesheet(
            Crane crane,
            LocalDate dateOfWork
    ) {
        JobCard jobCard = mock(JobCard.class);

        when(jobCard.getCraneId()).thenReturn(crane);

        Timesheet timesheet = mock(Timesheet.class);

        when(timesheet.getJobCard()).thenReturn(jobCard);
        when(timesheet.getDateOfWork()).thenReturn(dateOfWork);

        return timesheet;
    }

    private Maintenance createMaintenance(
            Crane crane,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Maintenance maintenance = mock(Maintenance.class);

        when(maintenance.getCrane()).thenReturn(crane);
        when(maintenance.getMaintenanceStartDate())
                .thenReturn(startDate);

        if (endDate != null) {
            when(maintenance.getMaintenanceEndDate())
                    .thenReturn(endDate);
        }

        return maintenance;
    }
}