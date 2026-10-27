package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CraneResponse;
import com.uvindu.crane_management_system.dto.CreateCraneRequest;
import com.uvindu.crane_management_system.dto.UpdateCraneRequest;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import com.uvindu.crane_management_system.repository.CraneRepository;
import com.uvindu.crane_management_system.util.CraneTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class CraneServiceTest extends BaseUnitTest {

    @Mock
    private CraneRepository craneRepository;

    @Mock
    private com.uvindu.crane_management_system.repository.MaintenanceRepository maintenanceRepository;

    @Mock
    private JobCardRepository jobCardRepository;

    @InjectMocks
    private CraneService craneService;

    @Test
    void shouldCreateCraneSuccessfully() {

        // Arrange
        CreateCraneRequest request = CraneTestDataBuilder.createCraneRequest();
        Crane savedCrane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);

        when(craneRepository.existsBySerialNumber("SN-NEW")).thenReturn(false);
        when(craneRepository.findTopByTypeOrderByCraneNumberDesc("MOBILE")).thenReturn(Optional.empty());
        when(craneRepository.save(any(Crane.class))).thenReturn(savedCrane);

        // Act
        CraneResponse response = craneService.createCrane(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCraneNumber()).isEqualTo("CRMOBILE001");
        assertThat(response.getType()).isEqualTo("MOBILE");
        assertThat(response.getStatus()).isEqualTo(CraneStatus.AVAILABLE);
        verify(craneRepository).save(any(Crane.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingCraneWithDuplicateSerialNumber() {

        // Arrange
        CreateCraneRequest request = CraneTestDataBuilder.createCraneRequest();
        when(craneRepository.existsBySerialNumber("SN-NEW")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> craneService.createCrane(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane already exists.");

        verify(craneRepository, never()).save(any(Crane.class));
    }


    @Test
    void shouldCreateCraneWithAvailableStatus() {

        // Arrange
        CreateCraneRequest request = CraneTestDataBuilder.createCraneRequest();

        Crane savedCrane = CraneTestDataBuilder.createCrane(
                        1L,
                        "CRMOBILE001",
                        CraneStatus.AVAILABLE
                );

        when(craneRepository.existsBySerialNumber("SN-NEW")).thenReturn(false);
        when(craneRepository.findTopByTypeOrderByCraneNumberDesc("MOBILE")).thenReturn(Optional.empty());
        when(craneRepository.save(any(Crane.class))).thenReturn(savedCrane);

        // Act
        craneService.createCrane(request);

        // Assert
        ArgumentCaptor<Crane> craneCaptor = ArgumentCaptor.forClass(Crane.class);

        verify(craneRepository).save(craneCaptor.capture());

        Crane savedEntity = craneCaptor.getValue();

        assertThat(savedEntity.getStatus()).isEqualTo(CraneStatus.AVAILABLE);
    }


    @Test
    void shouldGenerateCraneNumberWithExistingCranes() {

        // Arrange
        CreateCraneRequest request = CraneTestDataBuilder.createCraneRequest();
        Crane existingCrane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE005", CraneStatus.AVAILABLE);
        Crane savedCrane = CraneTestDataBuilder.createCrane(2L, "CRMOBILE006", CraneStatus.AVAILABLE);

        when(craneRepository.existsBySerialNumber("SN-NEW")).thenReturn(false);
        when(craneRepository.findTopByTypeOrderByCraneNumberDesc("MOBILE")).thenReturn(Optional.of(existingCrane));
        when(craneRepository.save(any(Crane.class))).thenReturn(savedCrane);

        // Act
        CraneResponse response = craneService.createCrane(request);

        // Assert
        assertThat(response.getCraneNumber()).isEqualTo("CRMOBILE006");
        verify(craneRepository).findTopByTypeOrderByCraneNumberDesc("MOBILE");
    }


    @Test
    void shouldGenerateCraneNumberAfterExistingCrane() {

        // Arrange
        CreateCraneRequest request = CraneTestDataBuilder.createCraneRequest();

        Crane existingCrane =
                CraneTestDataBuilder.createCrane(
                        1L,
                        "CRMOBILE009",
                        CraneStatus.AVAILABLE
                );

        Crane savedCrane =
                CraneTestDataBuilder.createCrane(
                        2L,
                        "CRMOBILE010",
                        CraneStatus.AVAILABLE
                );

        when(craneRepository.existsBySerialNumber("SN-NEW")).thenReturn(false);
        when(craneRepository.findTopByTypeOrderByCraneNumberDesc("MOBILE")).thenReturn(Optional.of(existingCrane));
        when(craneRepository.save(any(Crane.class))).thenReturn(savedCrane);

        // Act
        CraneResponse response = craneService.createCrane(request);

        // Assert
        assertThat(response.getCraneNumber()).isEqualTo("CRMOBILE010");
    }


    @Test
    void shouldGetAllCranes() {

        // Arrange
        Crane crane1 = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        Crane crane2 = CraneTestDataBuilder.createCrane(2L, "CRTOWER001", CraneStatus.IN_USE);
        when(craneRepository.findAll()).thenReturn(Arrays.asList(crane1, crane2));

        // Act
        List<CraneResponse> responses = craneService.getAllCranes();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getCraneNumber()).isEqualTo("CRMOBILE001");
        assertThat(responses.get(1).getCraneNumber()).isEqualTo("CRTOWER001");
        verify(craneRepository).findAll();
    }

    @Test
    void shouldGetCraneById() {

        // Arrange
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));

        // Act
        CraneResponse response = craneService.getCraneById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCraneNumber()).isEqualTo("CRMOBILE001");
        verify(craneRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentCrane() {

        // Arrange
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> craneService.getCraneById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");
    }

    @Test
    void shouldUpdateCrane() {

        // Arrange
        Crane existingCrane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        UpdateCraneRequest request = CraneTestDataBuilder.createUpdateCraneRequest();

        when(craneRepository.findById(1L)).thenReturn(Optional.of(existingCrane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CraneResponse response = craneService.updateCrane(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("TOWER");
        assertThat(response.getCapacity()).isEqualTo(100.0);
        assertThat(response.getManufacturer()).isEqualTo("Potain");
        verify(craneRepository).save(any(Crane.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCrane() {

        // Arrange
        UpdateCraneRequest request = CraneTestDataBuilder.createUpdateCraneRequest();
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> craneService.updateCrane(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");
    }


    @Test
    void shouldPreserveCraneNumberWhenUpdatingCrane() {

        // Arrange
        Crane existingCrane =
                CraneTestDataBuilder.createCrane(
                        1L,
                        "CRMOBILE001",
                        CraneStatus.AVAILABLE
                );

        UpdateCraneRequest request = CraneTestDataBuilder.createUpdateCraneRequest();

        when(craneRepository.findById(1L)).thenReturn(Optional.of(existingCrane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CraneResponse response = craneService.updateCrane(1L, request);

        // Assert
        assertThat(response.getCraneNumber()).isEqualTo("CRMOBILE001");
    }


    @Test
    void shouldMarkCraneAsAvailable() {

        // Arrange
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.IN_USE);
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        craneService.markAsAvailable(1L);

        // Assert
        assertThat(crane.getStatus()).isEqualTo(CraneStatus.AVAILABLE);
        verify(craneRepository).save(crane);
    }


    @Test
    void shouldThrowExceptionWhenMarkingNonExistentCraneAsAvailable() {

        // Arrange
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                craneService.markAsAvailable(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");

        verify(craneRepository, never()).save(any(Crane.class));
    }


    @Test
    void shouldMarkCraneAsInUse() {

        // Arrange
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        craneService.markAsInUse(1L);

        // Assert
        assertThat(crane.getStatus()).isEqualTo(CraneStatus.IN_USE);
        verify(craneRepository).save(crane);
    }


    @Test
    void shouldThrowExceptionWhenMarkingNonExistentCraneAsInUse() {

        // Arrange
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                craneService.markAsInUse(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");

        verify(craneRepository, never()).save(any(Crane.class));
    }


    @Test
    void shouldMarkCraneAsMaintenance() {

        // Arrange
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        craneService.markAsMaintenance(1L);

        // Assert
        assertThat(crane.getStatus()).isEqualTo(CraneStatus.MAINTENANCE);
        verify(craneRepository).save(crane);
    }


    @Test
    void shouldThrowExceptionWhenMarkingNonExistentCraneAsMaintenance() {

        // Arrange
        when(craneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                craneService.markAsMaintenance(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");

        verify(craneRepository, never()).save(any(Crane.class));
    }


    @Test
    void shouldDeleteCrane() {

        // Arrange
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CRMOBILE001", CraneStatus.AVAILABLE);
        when(craneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(crane));
        when(craneRepository.save(any(Crane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        craneService.deleteCrane(1L);

        // Assert
        assertThat(crane.isActive()).isFalse();
        verify(craneRepository).save(crane);
    }


    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCrane() {

        // Arrange
        when(craneRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> craneService.deleteCrane(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");
    }

    @Test
    void activationPreservesNumberStatusAndFields() {
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CR080T001", CraneStatus.MAINTENANCE);
        when(craneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(crane));
        when(craneRepository.save(crane)).thenReturn(crane);
        for (boolean active : new boolean[]{false, false, true, true}) {
            CraneResponse result = craneService.updateActivation(1L, active);
            assertThat(result.isActive()).isEqualTo(active);
            assertThat(result.getCraneNumber()).isEqualTo("CR080T001");
            assertThat(result.getStatus()).isEqualTo(CraneStatus.MAINTENANCE);
            assertThat(result.getSerialNumber()).isEqualTo(crane.getSerialNumber());
        }
    }
    @Test
    void pendingJobsBlockBothDeactivationPaths() {
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CR080T001", CraneStatus.AVAILABLE);
        when(craneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(crane));
        when(jobCardRepository.existsByCraneId_IdAndStatus(1L, JobCardStatus.PENDING)).thenReturn(true);
        assertThatThrownBy(() -> craneService.updateActivation(1L, false)).hasMessageContaining("pending jobs");
        assertThatThrownBy(() -> craneService.deleteCrane(1L)).hasMessageContaining("pending jobs");
        assertThat(crane.isActive()).isTrue();
        verify(craneRepository, never()).save(any());
    }
    @Test
    void unfinishedMaintenanceBlocksDeactivation() {
        Crane crane = CraneTestDataBuilder.createCrane(1L, "CR080T001", CraneStatus.AVAILABLE);
        when(craneRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(crane));
        for (var status : new MaintenanceStatus[]{MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS}) {
            reset(maintenanceRepository);
            when(maintenanceRepository.existsByCrane_IdAndStatus(1L, status)).thenReturn(true);
            assertThatThrownBy(() -> craneService.updateActivation(1L, false)).hasMessageContaining("maintenance");
        }
        verify(craneRepository, never()).save(any());
    }
}
