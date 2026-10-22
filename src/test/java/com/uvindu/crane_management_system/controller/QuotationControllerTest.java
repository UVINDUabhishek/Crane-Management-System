package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateQuotationRequest;
import com.uvindu.crane_management_system.dto.QuotationResponse;
import com.uvindu.crane_management_system.dto.UpdateQuotationRequest;
import com.uvindu.crane_management_system.service.QuotationService;
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
class QuotationControllerTest {

    @Mock
    private QuotationService quotationService;

    @InjectMocks
    private QuotationController quotationController;

    // ==================== CREATE ====================

    @Test
    void shouldCreateQuotation() {

        CreateQuotationRequest request = new CreateQuotationRequest();
        QuotationResponse quotationResponse = new QuotationResponse();

        when(quotationService.createQuotation(request))
                .thenReturn(quotationResponse);

        ResponseEntity<QuotationResponse> response =
                quotationController.createQuotation(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(quotationResponse);

        verify(quotationService).createQuotation(request);
    }

    // ==================== GET ALL ====================

    @Test
    void shouldGetAllQuotations() {

        QuotationResponse quotation1 = new QuotationResponse();
        QuotationResponse quotation2 = new QuotationResponse();

        List<QuotationResponse> quotationList =
                List.of(quotation1, quotation2);

        when(quotationService.getAllQuotations())
                .thenReturn(quotationList);

        ResponseEntity<List<QuotationResponse>> response =
                quotationController.getAllQuotations();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(quotationList);
        assertThat(response.getBody()).hasSize(2);

        verify(quotationService).getAllQuotations();
    }

    // ==================== GET BY ID ====================

    @Test
    void shouldGetQuotationById() {

        Long quotationId = 1L;
        QuotationResponse quotationResponse = new QuotationResponse();

        when(quotationService.getQuotationById(quotationId))
                .thenReturn(quotationResponse);

        ResponseEntity<QuotationResponse> response =
                quotationController.getQuotationById(quotationId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(quotationResponse);

        verify(quotationService).getQuotationById(quotationId);
    }

    // ==================== UPDATE ====================

    @Test
    void shouldUpdateQuotation() {

        Long quotationId = 1L;
        UpdateQuotationRequest request = new UpdateQuotationRequest();
        QuotationResponse updatedQuotation = new QuotationResponse();

        when(quotationService.updateQuotation(quotationId, request))
                .thenReturn(updatedQuotation);

        ResponseEntity<QuotationResponse> response =
                quotationController.updateQuotation(quotationId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedQuotation);

        verify(quotationService).updateQuotation(quotationId, request);
    }

    // ==================== DELETE ====================

    @Test
    void shouldDeleteQuotation() {

        Long quotationId = 1L;

        ResponseEntity<Void> response =
                quotationController.deleteQuotation(quotationId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(quotationService).deleteQuotation(quotationId);
    }
}