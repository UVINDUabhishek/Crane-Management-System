package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateTimesheetRequest;
import com.uvindu.crane_management_system.dto.TimesheetResponse;
import com.uvindu.crane_management_system.dto.UpdateTimesheetRequest;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Timesheet;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import com.uvindu.crane_management_system.repository.TimesheetRepository;
import com.uvindu.crane_management_system.util.TimesheetTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TimesheetServiceTest extends BaseUnitTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private JobCardRepository jobCardRepository;

    @InjectMocks
    private TimesheetService timesheetService;

    @Test
    void shouldCreateTimesheetSuccessfully() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        CreateTimesheetRequest request = TimesheetTestDataBuilder.createTimesheetRequest(1L);
        Timesheet savedTimesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);

        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));
        when(timesheetRepository.existsByJobCardAndDateOfWork(jobCard, LocalDate.now())).thenReturn(false);
        when(timesheetRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(savedTimesheet);

        // Act
        TimesheetResponse response = timesheetService.createTimesheet(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTimesheetNumber()).isEqualTo("TS-0001");
        assertThat(response.getJobCardNumber()).isEqualTo("JC-0001");
        verify(timesheetRepository).save(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingTimesheetForNonExistentJobCard() {
        // Arrange
        CreateTimesheetRequest request = TimesheetTestDataBuilder.createTimesheetRequest(999L);
        when(jobCardRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.createTimesheet(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Job card not found");

        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingTimesheetForCompletedJobCard() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createCompletedJobCard(1L);
        CreateTimesheetRequest request = TimesheetTestDataBuilder.createTimesheetRequest(1L);

        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.createTimesheet(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet can only be added to a pending Job Card");
    }

    @Test
    void shouldThrowExceptionWhenCreatingTimesheetForCanceledJobCard() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        jobCard.setStatus(JobCardStatus.CANCELED);

        CreateTimesheetRequest request =
                TimesheetTestDataBuilder.createTimesheetRequest(1L);

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.createTimesheet(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet can only be added to a pending Job Card");

        verify(timesheetRepository, never())
                .save(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateTimesheetForSameDate() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        CreateTimesheetRequest request = TimesheetTestDataBuilder.createTimesheetRequest(1L);

        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));
        when(timesheetRepository.existsByJobCardAndDateOfWork(jobCard, LocalDate.now())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.createTimesheet(request))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode())
                                    .isEqualTo(HttpStatus.CONFLICT);

                            assertThat(exception.getReason())
                                    .isEqualTo("Timesheet already exists for this Job Card on this date");
                        }
                );

        verify(timesheetRepository, never()).save(any(Timesheet.class));
    }

    @Test
    void shouldGenerateTimesheetNumberWithExistingTimesheets() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        CreateTimesheetRequest request = TimesheetTestDataBuilder.createTimesheetRequest(1L);

        Timesheet existingTimesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0005", jobCard);
        Timesheet savedTimesheet = TimesheetTestDataBuilder.createTimesheet(2L, "TS-0006", jobCard);

        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));
        when(timesheetRepository.existsByJobCardAndDateOfWork(jobCard, LocalDate.now())).thenReturn(false);
        when(timesheetRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(existingTimesheet));
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(savedTimesheet);

        // Act
        TimesheetResponse response = timesheetService.createTimesheet(request);

        // Assert
        assertThat(response.getTimesheetNumber()).isEqualTo("TS-0006");
        verify(timesheetRepository).findTopByOrderByIdDesc();
    }

    @Test
    void shouldGetAllTimesheets() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet1 = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);
        Timesheet timesheet2 = TimesheetTestDataBuilder.createTimesheet(2L, "TS-0002", jobCard);

        when(timesheetRepository.findAll()).thenReturn(Arrays.asList(timesheet1, timesheet2));

        // Act
        List<TimesheetResponse> responses = timesheetService.getAllTimesheets();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTimesheetNumber()).isEqualTo("TS-0001");
        assertThat(responses.get(1).getTimesheetNumber()).isEqualTo("TS-0002");
        verify(timesheetRepository).findAll();
    }

    @Test
    void shouldGetTimesheetById() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        // Act
        TimesheetResponse response = timesheetService.getTimesheetById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTimesheetNumber()).isEqualTo("TS-0001");
        verify(timesheetRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentTimesheet() {
        // Arrange
        when(timesheetRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.getTimesheetById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found");
    }

    @Test
    void shouldGetTimesheetsByJobCard() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet1 = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);
        Timesheet timesheet2 = TimesheetTestDataBuilder.createTimesheet(2L, "TS-0002", jobCard);

        when(jobCardRepository.findById(1L)).thenReturn(Optional.of(jobCard));
        when(timesheetRepository.findByJobCard(jobCard)).thenReturn(Arrays.asList(timesheet1, timesheet2));

        // Act
        List<TimesheetResponse> responses = timesheetService.getTimesheetsByJobCard(1L);

        // Assert
        assertThat(responses).hasSize(2);
        verify(jobCardRepository).findById(1L);
        verify(timesheetRepository).findByJobCard(jobCard);
    }

    @Test
    void shouldUpdateTimesheet() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);
        UpdateTimesheetRequest request = TimesheetTestDataBuilder.createUpdateTimesheetRequest();

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.findByJobCardAndDateOfWork(jobCard, LocalDate.now().plusDays(1)))
                .thenReturn(Optional.empty());
        when(timesheetRepository.save(any(Timesheet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act
        TimesheetResponse response = timesheetService.updateTimesheet(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(timesheet.getWorkedHours()).isEqualTo(new BigDecimal("10"));
        assertThat(timesheet.getDescription()).isEqualTo("Updated timesheet");
        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingTimesheetForCompletedJobCard() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createCompletedJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);
        UpdateTimesheetRequest request = TimesheetTestDataBuilder.createUpdateTimesheetRequest();

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.updateTimesheet(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet cannot be updated because the Job Card is not pending");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentTimesheet() {
        // Arrange
        UpdateTimesheetRequest request =
                TimesheetTestDataBuilder.createUpdateTimesheetRequest();

        when(timesheetRepository.findJobCardIdById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.updateTimesheet(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found");

        verify(timesheetRepository, never())
                .save(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingTimesheetWithDuplicateDate() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);
        Timesheet existingTimesheet = TimesheetTestDataBuilder.createTimesheet(2L, "TS-0002", jobCard);

        UpdateTimesheetRequest request = TimesheetTestDataBuilder.createUpdateTimesheetRequest();

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));
        when(timesheetRepository.findByJobCardAndDateOfWork(jobCard, LocalDate.now().plusDays(1)))
                .thenReturn(Optional.of(existingTimesheet));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.updateTimesheet(1L, request))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode())
                                    .isEqualTo(HttpStatus.CONFLICT);

                            assertThat(exception.getReason())
                                    .isEqualTo("Timesheet already exists for this Job Card on this date");
                        }
                );
    }

    @Test
    void shouldAllowUpdatingTimesheetWhenDateBelongsToSameTimesheet() {
        // Arrange
        JobCard jobCard =
                TimesheetTestDataBuilder.createPendingJobCard(1L);

        Timesheet timesheet =
                TimesheetTestDataBuilder.createTimesheet(
                        1L,
                        "TS-0001",
                        jobCard
                );

        UpdateTimesheetRequest request =
                TimesheetTestDataBuilder.createUpdateTimesheetRequest();

        when(timesheetRepository.findById(1L))
                .thenReturn(Optional.of(timesheet));

        when(timesheetRepository.findByJobCardAndDateOfWork(
                jobCard,
                LocalDate.now().plusDays(1)))
                .thenReturn(Optional.of(timesheet));

        when(timesheetRepository.save(any(Timesheet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act
        TimesheetResponse response =
                timesheetService.updateTimesheet(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(timesheet.getDateOfWork())
                .isEqualTo(LocalDate.now().plusDays(1));

        verify(timesheetRepository).save(timesheet);
    }

    @Test
    void shouldDeleteTimesheet() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createPendingJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));
        doNothing().when(timesheetRepository).delete(timesheet);

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act
        timesheetService.deleteTimesheet(1L);

        // Assert
        verify(timesheetRepository).delete(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenDeletingTimesheetForCompletedJobCard() {
        // Arrange
        JobCard jobCard = TimesheetTestDataBuilder.createCompletedJobCard(1L);
        Timesheet timesheet = TimesheetTestDataBuilder.createTimesheet(1L, "TS-0001", jobCard);

        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.deleteTimesheet(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet cannot be deleted because the Job Card is not pending");

        verify(timesheetRepository, never()).delete(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingTimesheetForCanceledJobCard() {
        // Arrange
        JobCard jobCard =
                TimesheetTestDataBuilder.createCanceledJobCard(1L);

        Timesheet timesheet =
                TimesheetTestDataBuilder.createTimesheet(
                        1L,
                        "TS-0001",
                        jobCard
                );

        when(timesheetRepository.findById(1L))
                .thenReturn(Optional.of(timesheet));

        when(timesheetRepository.findJobCardIdById(1L)).thenReturn(Optional.of(timesheet.getJobCard().getId()));
        when(jobCardRepository.findByIdForUpdate(timesheet.getJobCard().getId()))
                .thenReturn(Optional.of(timesheet.getJobCard()));

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.deleteTimesheet(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet cannot be deleted because the Job Card is not pending");

        verify(timesheetRepository, never())
                .delete(any(Timesheet.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentTimesheet() {
        // Arrange
        when(timesheetRepository.findJobCardIdById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> timesheetService.deleteTimesheet(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found");
    }
}
