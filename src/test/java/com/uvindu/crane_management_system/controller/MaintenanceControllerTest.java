package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateMaintenanceRequest;
import com.uvindu.crane_management_system.dto.MaintenanceResponse;
import com.uvindu.crane_management_system.dto.UpdateMaintenanceRequest;
import com.uvindu.crane_management_system.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceControllerTest {

    @Mock
    private MaintenanceService maintenanceService;

    @InjectMocks
    private MaintenanceController maintenanceController;


    // ---------------------------------------------------------
    // Create Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldCreateMaintenance() {

        // Arrange
        CreateMaintenanceRequest request =
                new CreateMaintenanceRequest();

        MaintenanceResponse maintenanceResponse =
                new MaintenanceResponse();

        when(maintenanceService.createMaintenance(request))
                .thenReturn(maintenanceResponse);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.createMaintenance(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(maintenanceResponse);

        verify(maintenanceService).createMaintenance(request);
    }


    // ---------------------------------------------------------
    // Get All Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldGetAllMaintenance() {

        // Arrange
        MaintenanceResponse maintenance1 =
                new MaintenanceResponse();

        MaintenanceResponse maintenance2 =
                new MaintenanceResponse();

        List<MaintenanceResponse> maintenanceList =
                List.of(maintenance1, maintenance2);

        when(maintenanceService.getAllMaintenance())
                .thenReturn(maintenanceList);

        // Act
        ResponseEntity<List<MaintenanceResponse>> response =
                maintenanceController.getAllMaintenance();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(maintenanceList);
        assertThat(response.getBody()).hasSize(2);

        verify(maintenanceService).getAllMaintenance();
    }


    // ---------------------------------------------------------
    // Get Maintenance By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetMaintenanceById() {

        // Arrange
        Long maintenanceId = 1L;

        MaintenanceResponse maintenanceResponse =
                new MaintenanceResponse();

        when(maintenanceService.getMaintenanceById(maintenanceId))
                .thenReturn(maintenanceResponse);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.getMaintenanceById(maintenanceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(maintenanceResponse);

        verify(maintenanceService).getMaintenanceById(maintenanceId);
    }


    // ---------------------------------------------------------
    // Update Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldUpdateMaintenance() {

        // Arrange
        Long maintenanceId = 1L;

        UpdateMaintenanceRequest request =
                new UpdateMaintenanceRequest();

        MaintenanceResponse updatedMaintenance =
                new MaintenanceResponse();

        when(maintenanceService.updateMaintenance(
                maintenanceId,
                request
        )).thenReturn(updatedMaintenance);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.updateMaintenance(
                        maintenanceId,
                        request
                );

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedMaintenance);

        verify(maintenanceService).updateMaintenance(
                maintenanceId,
                request
        );
    }


    // ---------------------------------------------------------
    // Start Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldStartMaintenance() {

        // Arrange
        Long maintenanceId = 1L;

        MaintenanceResponse startedMaintenance =
                new MaintenanceResponse();

        when(maintenanceService.startMaintenance(maintenanceId))
                .thenReturn(startedMaintenance);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.startMaintenance(maintenanceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(startedMaintenance);

        verify(maintenanceService).startMaintenance(maintenanceId);
    }


    // ---------------------------------------------------------
    // Complete Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldCompleteMaintenance() {

        // Arrange
        Long maintenanceId = 1L;

        MaintenanceResponse completedMaintenance =
                new MaintenanceResponse();

        when(maintenanceService.completeMaintenance(maintenanceId))
                .thenReturn(completedMaintenance);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.completeMaintenance(maintenanceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(completedMaintenance);

        verify(maintenanceService).completeMaintenance(maintenanceId);
    }


    // ---------------------------------------------------------
    // Cancel Maintenance
    // ---------------------------------------------------------

    @Test
    void shouldCancelMaintenance() {

        // Arrange
        Long maintenanceId = 1L;

        MaintenanceResponse canceledMaintenance =
                new MaintenanceResponse();

        when(maintenanceService.cancelMaintenance(maintenanceId))
                .thenReturn(canceledMaintenance);

        // Act
        ResponseEntity<MaintenanceResponse> response =
                maintenanceController.cancelMaintenance(maintenanceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(canceledMaintenance);

        verify(maintenanceService).cancelMaintenance(maintenanceId);
    }
}
