package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateJobCardRequest;
import com.uvindu.crane_management_system.dto.JobCardResponse;
import com.uvindu.crane_management_system.dto.UpdateJobCardRequest;
import com.uvindu.crane_management_system.service.JobCardService;
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
class JobCardControllerTest {

    @Mock
    private JobCardService jobCardService;

    @InjectMocks
    private JobCardController jobCardController;


    // ---------------------------------------------------------
    // Create Job Card
    // ---------------------------------------------------------

    @Test
    void shouldCreateJobCard() {

        // Arrange
        CreateJobCardRequest request =
                new CreateJobCardRequest();

        JobCardResponse jobCardResponse =
                new JobCardResponse();

        when(jobCardService.createJobCard(request))
                .thenReturn(jobCardResponse);

        // Act
        ResponseEntity<JobCardResponse> response =
                jobCardController.createJobCard(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(jobCardResponse);

        verify(jobCardService).createJobCard(request);
    }


    // ---------------------------------------------------------
    // Get All Job Cards
    // ---------------------------------------------------------

    @Test
    void shouldGetAllJobCards() {

        // Arrange
        JobCardResponse jobCard1 =
                new JobCardResponse();

        JobCardResponse jobCard2 =
                new JobCardResponse();

        List<JobCardResponse> jobCards =
                List.of(jobCard1, jobCard2);

        when(jobCardService.getAllJobCards())
                .thenReturn(jobCards);

        // Act
        ResponseEntity<List<JobCardResponse>> response =
                jobCardController.getAllJobCards();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(jobCards);
        assertThat(response.getBody()).hasSize(2);

        verify(jobCardService).getAllJobCards();
    }


    // ---------------------------------------------------------
    // Get Job Card By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetJobCardById() {

        // Arrange
        Long jobCardId = 1L;

        JobCardResponse jobCardResponse =
                new JobCardResponse();

        when(jobCardService.getJobCardById(jobCardId))
                .thenReturn(jobCardResponse);

        // Act
        ResponseEntity<JobCardResponse> response =
                jobCardController.getJobCardById(jobCardId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(jobCardResponse);

        verify(jobCardService).getJobCardById(jobCardId);
    }


    // ---------------------------------------------------------
    // Update Job Card
    // ---------------------------------------------------------

    @Test
    void shouldUpdateJobCard() {

        // Arrange
        Long jobCardId = 1L;

        UpdateJobCardRequest request =
                new UpdateJobCardRequest();

        JobCardResponse updatedJobCard =
                new JobCardResponse();

        when(jobCardService.updateJobCard(jobCardId, request))
                .thenReturn(updatedJobCard);

        // Act
        ResponseEntity<JobCardResponse> response =
                jobCardController.updateJobCard(
                        jobCardId,
                        request
                );

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedJobCard);

        verify(jobCardService).updateJobCard(
                jobCardId,
                request
        );
    }


    // ---------------------------------------------------------
    // Complete Job Card
    // ---------------------------------------------------------

    @Test
    void shouldCompleteJobCard() {

        // Arrange
        Long jobCardId = 1L;

        JobCardResponse completedJobCard =
                new JobCardResponse();

        when(jobCardService.completeJobCard(jobCardId))
                .thenReturn(completedJobCard);

        // Act
        ResponseEntity<JobCardResponse> response =
                jobCardController.completeJobCard(jobCardId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(completedJobCard);

        verify(jobCardService).completeJobCard(jobCardId);
    }


    // ---------------------------------------------------------
    // Cancel Job Card
    // ---------------------------------------------------------

    @Test
    void shouldCancelJobCard() {

        // Arrange
        Long jobCardId = 1L;

        JobCardResponse canceledJobCard =
                new JobCardResponse();

        when(jobCardService.cancelJobCard(jobCardId))
                .thenReturn(canceledJobCard);

        // Act
        ResponseEntity<JobCardResponse> response =
                jobCardController.cancelJobCard(jobCardId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(canceledJobCard);

        verify(jobCardService).cancelJobCard(jobCardId);
    }
}