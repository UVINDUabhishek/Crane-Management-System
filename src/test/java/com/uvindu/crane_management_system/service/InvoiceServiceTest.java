package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateInvoiceRequest;
import com.uvindu.crane_management_system.dto.InvoiceResponse;
import com.uvindu.crane_management_system.dto.UpdateInvoiceRequest;
import com.uvindu.crane_management_system.model.Invoice;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.repository.InvoiceRepository;
import com.uvindu.crane_management_system.util.InvoiceTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceServiceTest extends BaseUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void shouldCreateInvoiceFromJobCardSuccessfully() {
        // Arrange
        JobCard jobCard = InvoiceTestDataBuilder.createCompletedJobCardWithInvoiceData();
        Invoice savedInvoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        savedInvoice.setCustomer(jobCard.getCustomerId());
        savedInvoice.setCrane(jobCard.getCraneId());
        savedInvoice.setQuotation(jobCard.getQuotationId());
        savedInvoice.setJobCard(jobCard);

        when(invoiceRepository.existsByJobCard(jobCard)).thenReturn(false);
        when(invoiceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        // Act
        InvoiceResponse response = invoiceService.createInvoiceFromJobCard(jobCard);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-0001");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.getVersion()).isEqualTo(1);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateInvoiceForJobCard() {
        // Arrange
        JobCard jobCard = InvoiceTestDataBuilder.createCompletedJobCardWithInvoiceData();
        when(invoiceRepository.existsByJobCard(jobCard)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.createInvoiceFromJobCard(jobCard))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invoice already exists for this Job Card");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingInvoiceWithZeroBillableHours() {
        // Arrange
        JobCard jobCard = InvoiceTestDataBuilder.createCompletedJobCardWithInvoiceData();
        jobCard.setTotalBillableHours(BigDecimal.ZERO);

        when(invoiceRepository.existsByJobCard(jobCard)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.createInvoiceFromJobCard(jobCard))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Total billable hours are not available for this Job-card");
    }

    @Test
    void shouldThrowExceptionWhenCreatingInvoiceWithZeroRate() {
        // Arrange
        JobCard jobCard = InvoiceTestDataBuilder.createCompletedJobCardWithInvoiceData();
        jobCard.getQuotationId().setMinRate(BigDecimal.ZERO);

        when(invoiceRepository.existsByJobCard(jobCard)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.createInvoiceFromJobCard(jobCard))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Minimum rate is not available in the quotation");
    }

    @Test
    void shouldGenerateInvoiceNumberWithExistingInvoices() {
        // Arrange
        JobCard jobCard = InvoiceTestDataBuilder.createCompletedJobCardWithInvoiceData();

        Invoice existingInvoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0005", InvoiceStatus.DRAFT);
        Invoice savedInvoice = InvoiceTestDataBuilder.createInvoice(2L, "INV-0006", InvoiceStatus.DRAFT);
        savedInvoice.setCustomer(jobCard.getCustomerId());
        savedInvoice.setCrane(jobCard.getCraneId());
        savedInvoice.setQuotation(jobCard.getQuotationId());
        savedInvoice.setJobCard(jobCard);

        when(invoiceRepository.existsByJobCard(jobCard)).thenReturn(false);
        when(invoiceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        // Act
        InvoiceResponse response = invoiceService.createInvoiceFromJobCard(jobCard);

        // Assert
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-0006");
        verify(invoiceRepository).findTopByOrderByIdDesc();
    }

    @Test
    void shouldCompleteInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.completeInvoice(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.COMPLETED);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldThrowExceptionWhenCompletingAlreadyCompletedInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.COMPLETED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.completeInvoice(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invoice is already completed");
    }

    @Test
    void shouldThrowExceptionWhenCompletingRevisedInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.REVISED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.completeInvoice(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Revised invoice cannot be completed");
    }

    @Test
    void shouldGetAllInvoices() {
        // Arrange
        Invoice invoice1 = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        Invoice invoice2 = InvoiceTestDataBuilder.createInvoice(2L, "INV-0002", InvoiceStatus.COMPLETED);

        when(invoiceRepository.findAll()).thenReturn(Arrays.asList(invoice1, invoice2));

        // Act
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getInvoiceNumber()).isEqualTo("INV-0001");
        assertThat(responses.get(1).getInvoiceNumber()).isEqualTo("INV-0002");
        verify(invoiceRepository).findAll();
    }

    @Test
    void shouldGetInvoiceById() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act
        InvoiceResponse response = invoiceService.getInvoiceById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-0001");
        verify(invoiceRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentInvoice() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void shouldUpdateInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        UpdateInvoiceRequest request = InvoiceTestDataBuilder.createUpdateInvoiceRequest();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.updateInvoice(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(invoice.getQty()).isEqualTo(new BigDecimal("42"));
        assertThat(invoice.getRate()).isEqualTo(new BigDecimal("5200"));
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonDraftInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.COMPLETED);
        UpdateInvoiceRequest request = InvoiceTestDataBuilder.createUpdateInvoiceRequest();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.updateInvoice(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only draft invoices can be updated");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingInvoiceWithZeroQty() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        UpdateInvoiceRequest request = InvoiceTestDataBuilder.createUpdateInvoiceRequest();
        request.setQty(BigDecimal.ZERO);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.updateInvoice(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("qty must be greater than 0");
    }

    @Test
    void shouldCancelInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.cancelInvoice(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldThrowExceptionWhenCancelingAlreadyCanceledInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.CANCELED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.cancelInvoice(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invoice is already canceled");
    }

    @Test
    void shouldThrowExceptionWhenCancelingRevisedInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.REVISED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.cancelInvoice(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Revised invoice cannot be canceled");
    }

    @Test
    void shouldReviseInvoice() {
        // Arrange
        Invoice oldInvoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.COMPLETED);
        Invoice newInvoice = InvoiceTestDataBuilder.createInvoice(2L, "INV-0002", InvoiceStatus.DRAFT);
        newInvoice.setVersion(2);

        CreateInvoiceRequest request = InvoiceTestDataBuilder.createInvoiceRequest();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(oldInvoice));
        when(invoiceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(oldInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(newInvoice);

        // Act
        InvoiceResponse response = invoiceService.reviseInvoice(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.getVersion()).isEqualTo(2);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenRevisingNonCompletedInvoice() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.DRAFT);
        CreateInvoiceRequest request = InvoiceTestDataBuilder.createInvoiceRequest();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.reviseInvoice(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only a completed invoice can be revised");
    }

    @Test
    void shouldThrowExceptionWhenRevisingInvoiceWithoutQtyAndRate() {
        // Arrange
        Invoice invoice = InvoiceTestDataBuilder.createInvoice(1L, "INV-0001", InvoiceStatus.COMPLETED);
        CreateInvoiceRequest request = new CreateInvoiceRequest();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.reviseInvoice(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quantity and rate are required");
    }
}
