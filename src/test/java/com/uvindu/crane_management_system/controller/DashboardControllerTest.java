package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.DashboardResponse;
import com.uvindu.crane_management_system.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;


    // ---------------------------------------------------------
    // Get Dashboard
    // ---------------------------------------------------------

    @Test
    void shouldGetDashboard() {

        // Arrange
        DashboardResponse dashboardResponse =
                new DashboardResponse();

        when(dashboardService.getDashboard())
                .thenReturn(dashboardResponse);

        // Act
        ResponseEntity<DashboardResponse> response =
                dashboardController.getDashboard();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dashboardResponse);

        verify(dashboardService).getDashboard();
    }
}