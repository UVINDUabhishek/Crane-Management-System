package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateInvoiceRequest;
import com.uvindu.crane_management_system.dto.InvoiceResponse;
import com.uvindu.crane_management_system.dto.UpdateInvoiceRequest;
import com.uvindu.crane_management_system.service.InvoiceService;
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
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;


    // ---------------------------------------------------------
    // Get All Invoices
    // ---------------------------------------------------------

    @Test
    void shouldGetAllInvoices() {

        // Arrange
        InvoiceResponse invoice1 = new InvoiceResponse();
        InvoiceResponse invoice2 = new InvoiceResponse();

        List<InvoiceResponse> invoices =
                List.of(invoice1, invoice2);

        when(invoiceService.getAllInvoices())
                .thenReturn(invoices);

        // Act
        ResponseEntity<List<InvoiceResponse>> response =
                invoiceController.getAllInvoices();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(invoices);
        assertThat(response.getBody()).hasSize(2);

        verify(invoiceService).getAllInvoices();
    }


    // ---------------------------------------------------------
    // Get Invoice By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetInvoiceById() {

        // Arrange
        Long invoiceId = 1L;

        InvoiceResponse invoiceResponse =
                new InvoiceResponse();

        when(invoiceService.getInvoiceById(invoiceId))
                .thenReturn(invoiceResponse);

        // Act
        ResponseEntity<InvoiceResponse> response =
                invoiceController.getInvoiceById(invoiceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(invoiceResponse);

        verify(invoiceService).getInvoiceById(invoiceId);
    }


    // ---------------------------------------------------------
    // Update Invoice
    // ---------------------------------------------------------

    @Test
    void shouldUpdateInvoice() {

        // Arrange
        Long invoiceId = 1L;

        UpdateInvoiceRequest request =
                new UpdateInvoiceRequest();

        InvoiceResponse updatedInvoice =
                new InvoiceResponse();

        when(invoiceService.updateInvoice(invoiceId, request))
                .thenReturn(updatedInvoice);

        // Act
        ResponseEntity<InvoiceResponse> response =
                invoiceController.updateInvoice(invoiceId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedInvoice);

        verify(invoiceService).updateInvoice(
                invoiceId,
                request
        );
    }


    // ---------------------------------------------------------
    // Complete Invoice
    // ---------------------------------------------------------

    @Test
    void shouldCompleteInvoice() {

        // Arrange
        Long invoiceId = 1L;

        InvoiceResponse completedInvoice =
                new InvoiceResponse();

        when(invoiceService.completeInvoice(invoiceId))
                .thenReturn(completedInvoice);

        // Act
        ResponseEntity<InvoiceResponse> response =
                invoiceController.completeInvoice(invoiceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(completedInvoice);

        verify(invoiceService).completeInvoice(invoiceId);
    }


    // ---------------------------------------------------------
    // Cancel Invoice
    // ---------------------------------------------------------

    @Test
    void shouldCancelInvoice() {

        // Arrange
        Long invoiceId = 1L;

        InvoiceResponse canceledInvoice =
                new InvoiceResponse();

        when(invoiceService.cancelInvoice(invoiceId))
                .thenReturn(canceledInvoice);

        // Act
        ResponseEntity<InvoiceResponse> response =
                invoiceController.cancelInvoice(invoiceId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(canceledInvoice);

        verify(invoiceService).cancelInvoice(invoiceId);
    }


    // ---------------------------------------------------------
    // Revise Invoice
    // ---------------------------------------------------------

    @Test
    void shouldReviseInvoice() {

        // Arrange
        Long invoiceId = 1L;

        CreateInvoiceRequest request =
                new CreateInvoiceRequest();

        InvoiceResponse revisedInvoice =
                new InvoiceResponse();

        when(invoiceService.reviseInvoice(invoiceId, request))
                .thenReturn(revisedInvoice);

        // Act
        ResponseEntity<InvoiceResponse> response =
                invoiceController.reviseInvoice(invoiceId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(revisedInvoice);

        verify(invoiceService).reviseInvoice(
                invoiceId,
                request
        );
    }
}