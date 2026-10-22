package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateCraneRequest;
import com.uvindu.crane_management_system.dto.CraneResponse;
import com.uvindu.crane_management_system.dto.UpdateCraneRequest;
import com.uvindu.crane_management_system.service.CraneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CraneControllerTest {

    @Mock
    private CraneService craneService;

    @InjectMocks
    private CraneController craneController;


    // ---------------------------------------------------------
    // Create Crane
    // ---------------------------------------------------------

    @Test
    void shouldCreateCrane() {

        // Arrange
        CreateCraneRequest request = new CreateCraneRequest();

        CraneResponse craneResponse = new CraneResponse();

        when(craneService.createCrane(request))
                .thenReturn(craneResponse);

        // Act
        ResponseEntity<CraneResponse> response =
                craneController.createCrane(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(craneResponse);

        verify(craneService).createCrane(request);
    }


    // ---------------------------------------------------------
    // Get All Cranes
    // ---------------------------------------------------------

    @Test
    void shouldGetAllCranes() {

        // Arrange
        CraneResponse crane1 = new CraneResponse();
        CraneResponse crane2 = new CraneResponse();

        List<CraneResponse> cranes =
                List.of(crane1, crane2);

        when(craneService.getAllCranes())
                .thenReturn(cranes);

        // Act
        ResponseEntity<List<CraneResponse>> response =
                craneController.getAllCranes();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(cranes);
        assertThat(response.getBody()).hasSize(2);

        verify(craneService).getAllCranes();
    }


    // ---------------------------------------------------------
    // Get Crane By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetCraneById() {

        // Arrange
        Long craneId = 1L;

        CraneResponse craneResponse =
                new CraneResponse();

        when(craneService.getCraneById(craneId))
                .thenReturn(craneResponse);

        // Act
        ResponseEntity<CraneResponse> response =
                craneController.getCraneById(craneId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(craneResponse);

        verify(craneService).getCraneById(craneId);
    }


    // ---------------------------------------------------------
    // Update Crane
    // ---------------------------------------------------------

    @Test
    void shouldUpdateCrane() {

        // Arrange
        Long craneId = 1L;

        UpdateCraneRequest request =
                new UpdateCraneRequest();

        CraneResponse updatedCrane =
                new CraneResponse();

        when(craneService.updateCrane(craneId, request))
                .thenReturn(updatedCrane);

        // Act
        ResponseEntity<CraneResponse> response =
                craneController.updateCrane(craneId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedCrane);

        verify(craneService).updateCrane(craneId, request);
    }


    // ---------------------------------------------------------
    // Delete Crane
    // ---------------------------------------------------------

    @Test
    void shouldDeleteCrane() {

        // Arrange
        Long craneId = 1L;

        // Act
        ResponseEntity<Void> response =
                craneController.deleteCrane(craneId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(craneService).deleteCrane(craneId);
    }
}