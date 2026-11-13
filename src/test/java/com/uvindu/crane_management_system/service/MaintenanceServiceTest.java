package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateMaintenanceRequest;
import com.uvindu.crane_management_system.dto.MaintenanceResponse;
import com.uvindu.crane_management_system.dto.UpdateMaintenanceRequest;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.repository.CraneRepository;
import com.uvindu.crane_management_system.repository.MaintenanceRepository;
import com.uvindu.crane_management_system.repository.UserRepository;
import com.uvindu.crane_management_system.util.MaintenanceTestDataBuilder;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MaintenanceServiceTest extends BaseUnitTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private CraneRepository craneRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CraneService craneService;

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = MaintenanceStatus.class, names = {"COMPLETED", "CANCELED"})
    void terminalUpdateHasNoSideEffects(MaintenanceStatus status) {
        Maintenance record = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-1", status);
        String description = record.getDescription();
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(record));
        assertThatThrownBy(() -> maintenanceService.updateMaintenance(1L,
                MaintenanceTestDataBuilder.createUpdateMaintenanceRequest(1L)))
                .hasMessage("Completed or canceled maintenance cannot be updated");
        assertThat(record.getDescription()).isEqualTo(description);
        assertThat(record.getStatus()).isEqualTo(status);
        verify(maintenanceRepository, never()).save(any());
        verifyNoInteractions(craneRepository, userRepository, craneService, schedulingService);
    }

    @Test
    void shouldCreateMaintenanceSuccessfully() {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        User maintenancePerson = MaintenanceTestDataBuilder.createMaintenancePerson(1L);
        CreateMaintenanceRequest request = MaintenanceTestDataBuilder.createMaintenanceRequest(1L);
        Maintenance savedMaintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.SCHEDULED);
        savedMaintenance.setCrane(crane);
        savedMaintenance.setMaintenancePerson(maintenancePerson);

        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(userRepository.findById(1L)).thenReturn(Optional.of(maintenancePerson));
        when(maintenanceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(savedMaintenance);

        // Act
        MaintenanceResponse response = maintenanceService.createMaintenance(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMaintenanceNumber()).isEqualTo("MT-0001");
        assertThat(response.getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED);
        verify(maintenanceRepository).save(any(Maintenance.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingMaintenanceWithNonExistentCrane() {
        // Arrange
        CreateMaintenanceRequest request = MaintenanceTestDataBuilder.createMaintenanceRequest(999L);
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.createMaintenance(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");

        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }


    @Test
    void shouldThrowExceptionWhenCreatingMaintenanceWithNonExistentMaintenancePerson() {

        // Arrange
        CreateMaintenanceRequest request = MaintenanceTestDataBuilder.createMaintenanceRequest(1L);
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);

        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.createMaintenance(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance person not found");

        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }


    @Test
    void shouldGenerateMaintenanceNumberWithExistingMaintenances() {

        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        CreateMaintenanceRequest request = MaintenanceTestDataBuilder.createMaintenanceRequest(1L);
        User maintenancePerson = MaintenanceTestDataBuilder.createMaintenancePerson(1L);

        Maintenance existingMaintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0005",
                        MaintenanceStatus.SCHEDULED
                );

        Maintenance savedMaintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        2L,
                        "MT-0006",
                        MaintenanceStatus.SCHEDULED
                );

        savedMaintenance.setCrane(crane);

        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(userRepository.findById(1L)).thenReturn(Optional.of(maintenancePerson));
        when(maintenanceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(existingMaintenance));
        when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(savedMaintenance);

        // Act
        MaintenanceResponse response = maintenanceService.createMaintenance(request);

        // Assert
        assertThat(response.getMaintenanceNumber()).isEqualTo("MT-0006");
        verify(maintenanceRepository).findTopByOrderByIdDesc();
    }

    @Test
    void shouldGetAllMaintenance() {

        // Arrange
        Maintenance maintenance1 = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.SCHEDULED);
        Maintenance maintenance2 = MaintenanceTestDataBuilder.createMaintenance(2L, "MT-0002", MaintenanceStatus.COMPLETED);

        maintenance1.setCrane(MaintenanceTestDataBuilder.createCrane(1L));
        maintenance2.setCrane(MaintenanceTestDataBuilder.createCrane(2L));

        when(maintenanceRepository.findAll()).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // Act
        List<MaintenanceResponse> responses = maintenanceService.getAllMaintenance();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getMaintenanceNumber()).isEqualTo("MT-0001");
        assertThat(responses.get(1).getMaintenanceNumber()).isEqualTo("MT-0002");
        verify(maintenanceRepository).findAll();
    }

    @Test
    void shouldGetMaintenanceById() {

        // Arrange
        Maintenance maintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        MaintenanceStatus.SCHEDULED
                );

        maintenance.setCrane(MaintenanceTestDataBuilder.createCrane(1L));

        when(maintenanceRepository.findById(1L))
                .thenReturn(Optional.of(maintenance));

        // Act
        MaintenanceResponse response = maintenanceService.getMaintenanceById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMaintenanceNumber()).isEqualTo("MT-0001");
        verify(maintenanceRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentMaintenance() {
        // Arrange
        when(maintenanceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.getMaintenanceById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance not found");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = MaintenanceStatus.class, names = {"SCHEDULED", "IN_PROGRESS"})
    void shouldUpdateMaintenance(MaintenanceStatus state) {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        User maintenancePerson = MaintenanceTestDataBuilder.createMaintenancePerson(2L);
        Maintenance existingMaintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", state);
        existingMaintenance.setCrane(crane);

        UpdateMaintenanceRequest request = MaintenanceTestDataBuilder.createUpdateMaintenanceRequest(1L);

        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingMaintenance));
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(userRepository.findById(2L)).thenReturn(Optional.of(maintenancePerson));
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MaintenanceResponse response = maintenanceService.updateMaintenance(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(existingMaintenance.getDescription()).isEqualTo("Updated maintenance");
        verify(maintenanceRepository).save(any(Maintenance.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentMaintenance() {
        // Arrange
        UpdateMaintenanceRequest request = MaintenanceTestDataBuilder.createUpdateMaintenanceRequest(1L);
        when(maintenanceRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.updateMaintenance(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance not found");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNonExistentMaintenancePerson() {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        Maintenance maintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        MaintenanceStatus.SCHEDULED
                );

        maintenance.setCrane(crane);

        UpdateMaintenanceRequest request =
                MaintenanceTestDataBuilder.createUpdateMaintenanceRequest(1L);

        when(maintenanceRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(maintenance));

        when(craneRepository.findById(1L))
                .thenReturn(Optional.of(crane));

        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.updateMaintenance(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance person not found");

        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void shouldStartMaintenance() {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.SCHEDULED);
        maintenance.setCrane(crane);

        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(craneService).markAsMaintenance(1L);

        // Act
        MaintenanceResponse response = maintenanceService.startMaintenance(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        verify(craneService).markAsMaintenance(1L);
        verify(maintenanceRepository).save(maintenance);
    }

    @Test
    void shouldThrowExceptionWhenStartingCompletedMaintenance() {
        // Arrange
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.COMPLETED);
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.startMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Completed maintenance cannot be started");
    }

    @Test
    void shouldThrowExceptionWhenStartingCanceledMaintenance() {
        // Arrange
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.CANCELED);
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.startMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Canceled maintenance cannot be started");
    }

    @Test
    void shouldThrowExceptionWhenStartingAlreadyInProgressMaintenance() {
        // Arrange
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.IN_PROGRESS);
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.startMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance is already in progress");
    }

    @Test
    void shouldCompleteMaintenance() {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.IN_PROGRESS);
        maintenance.setCrane(crane);

        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(craneService).markAsAvailable(1L);

        // Act
        MaintenanceResponse response = maintenanceService.completeMaintenance(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        verify(craneService).markAsAvailable(1L);
        verify(maintenanceRepository).save(maintenance);
    }

    @Test
    void shouldThrowExceptionWhenCompletingAlreadyCompletedMaintenance() {
        // Arrange
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.COMPLETED);
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.completeMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maintenance is already completed");
    }

    @Test
    void shouldThrowExceptionWhenCompletingCanceledMaintenance() {
        // Arrange
        Maintenance maintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        MaintenanceStatus.CANCELED
                );

        when(maintenanceRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.completeMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Canceled maintenance cannot be completed");

        verify(maintenanceRepository, never()).save(any(Maintenance.class));
        verifyNoInteractions(craneService);
    }

    @Test
    void shouldCancelMaintenance() {
        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.SCHEDULED);
        maintenance.setCrane(crane);

        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));
        when(maintenanceRepository.save(any(Maintenance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(craneService).markAsAvailable(1L);

        // Act
        MaintenanceResponse response = maintenanceService.cancelMaintenance(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(MaintenanceStatus.CANCELED);
        verify(craneService).markAsAvailable(1L);
        verify(maintenanceRepository).save(maintenance);
    }

    @Test
    void shouldThrowExceptionWhenCancelingCompletedMaintenance() {
        // Arrange
        Maintenance maintenance = MaintenanceTestDataBuilder.createMaintenance(1L, "MT-0001", MaintenanceStatus.COMPLETED);
        when(maintenanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(maintenance));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.cancelMaintenance(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Completed maintenance cannot be canceled");
    }

    @Test
    void shouldCheckCraneUnderMaintenance() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);

        when(maintenanceRepository.existsMaintenanceConflict(eq(1L), eq(startDate), eq(endDate), any()))
                .thenReturn(true);

        // Act
        boolean result = maintenanceService.isCraneUnderMaintenance(1L, startDate, endDate);

        // Assert
        assertThat(result).isTrue();
        verify(maintenanceRepository).existsMaintenanceConflict(eq(1L), eq(startDate), eq(endDate), any());
    }

    @Test
    void shouldReturnFalseWhenCraneNotUnderMaintenance() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);

        when(maintenanceRepository.existsMaintenanceConflict(eq(1L), eq(startDate), eq(endDate), any()))
                .thenReturn(false);

        // Act
        boolean result = maintenanceService.isCraneUnderMaintenance(1L, startDate, endDate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectCreatingMaintenanceForInactiveCrane() {

        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        crane.setActive(false);

        CreateMaintenanceRequest request =
                MaintenanceTestDataBuilder.createMaintenanceRequest(1L);

        when(craneRepository.findById(1L))
                .thenReturn(Optional.of(crane));

        // Act & Assert
        assertThatThrownBy(() -> maintenanceService.createMaintenance(request))
                .hasMessage("Cannot schedule maintenance for an inactive crane");

        verify(maintenanceRepository, never())
                .save(any(Maintenance.class));
    }

    @Test
    void shouldRejectUpdatingMaintenanceForInactiveCrane() {

        // Arrange
        Crane crane = MaintenanceTestDataBuilder.createCrane(1L);
        crane.setActive(false);

        Maintenance existingMaintenance =
                MaintenanceTestDataBuilder.createMaintenance(
                        1L,
                        "MT-0001",
                        MaintenanceStatus.SCHEDULED
                );

        existingMaintenance.setCrane(crane);

        UpdateMaintenanceRequest request =
                MaintenanceTestDataBuilder.createUpdateMaintenanceRequest(1L);

        String originalDescription = existingMaintenance.getDescription();
        MaintenanceStatus originalStatus = existingMaintenance.getStatus();

        when(maintenanceRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(existingMaintenance));

        when(craneRepository.findById(1L))
                .thenReturn(Optional.of(crane));

        // Act & Assert
        assertThatThrownBy(() ->
                maintenanceService.updateMaintenance(1L, request)
        )
                .hasMessage("Cannot update maintenance for an inactive crane");

        // No changes to the existing maintenance record
        assertThat(existingMaintenance.getDescription())
                .isEqualTo(originalDescription);

        assertThat(existingMaintenance.getStatus())
                .isEqualTo(originalStatus);

        assertThat(existingMaintenance.getCrane())
                .isSameAs(crane);

        verify(maintenanceRepository, never())
                .save(any(Maintenance.class));
    }
}
