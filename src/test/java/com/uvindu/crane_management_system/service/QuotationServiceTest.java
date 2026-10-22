package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateQuotationRequest;
import com.uvindu.crane_management_system.dto.QuotationResponse;
import com.uvindu.crane_management_system.dto.UpdateQuotationRequest;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.Quotation;
import com.uvindu.crane_management_system.repository.CustomerRepository;
import com.uvindu.crane_management_system.repository.QuotationRepository;
import com.uvindu.crane_management_system.util.QuotationTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuotationServiceTest extends BaseUnitTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private QuotationService quotationService;

    @Test
    void shouldCreateQuotationSuccessfully() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        CreateQuotationRequest request = QuotationTestDataBuilder.createQuotationRequest(1L);
        Quotation savedQuotation = QuotationTestDataBuilder.createQuotation(1L, "QT-0001", customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class))).thenReturn(savedQuotation);

        // Act
        QuotationResponse response = quotationService.createQuotation(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuotationNumber()).isEqualTo("QT-0001");
        assertThat(response.getCustomerName()).isEqualTo("ABC Construction");
        assertThat(response.getType()).isEqualTo("MOBILE");
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingQuotationWithNonExistentCustomer() {
        // Arrange
        CreateQuotationRequest request = QuotationTestDataBuilder.createQuotationRequest(999L);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quotationService.createQuotation(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer not found");

        verify(quotationRepository, never()).save(any(Quotation.class));
    }

    @Test
    void shouldGenerateQuotationNumberWithExistingQuotations() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        CreateQuotationRequest request = QuotationTestDataBuilder.createQuotationRequest(1L);
        Quotation existingQuotation = QuotationTestDataBuilder.createQuotation(1L, "QT-0005", customer);
        Quotation savedQuotation = QuotationTestDataBuilder.createQuotation(2L, "QT-0006", customer);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(existingQuotation));
        when(quotationRepository.save(any(Quotation.class))).thenReturn(savedQuotation);

        // Act
        QuotationResponse response = quotationService.createQuotation(request);

        // Assert
        assertThat(response.getQuotationNumber()).isEqualTo("QT-0006");
        verify(quotationRepository).findTopByOrderByIdDesc();
    }

    @Test
    void shouldGetAllQuotations() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation1 = QuotationTestDataBuilder.createQuotation(1L, "QT-0001", customer);
        Quotation quotation2 = QuotationTestDataBuilder.createQuotation(2L, "QT-0002", customer);

        when(quotationRepository.findAll()).thenReturn(Arrays.asList(quotation1, quotation2));

        // Act
        List<QuotationResponse> responses = quotationService.getAllQuotations();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getQuotationNumber()).isEqualTo("QT-0001");
        assertThat(responses.get(1).getQuotationNumber()).isEqualTo("QT-0002");
        verify(quotationRepository).findAll();
    }

    @Test
    void shouldGetQuotationById() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = QuotationTestDataBuilder.createQuotation(1L, "QT-0001", customer);
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));

        // Act
        QuotationResponse response = quotationService.getQuotationById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuotationNumber()).isEqualTo("QT-0001");
        assertThat(response.getCustomerName()).isEqualTo("ABC Construction");
        verify(quotationRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentQuotation() {
        // Arrange
        when(quotationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quotationService.getQuotationById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quotation not found");
    }

    @Test
    void shouldUpdateQuotation() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation existingQuotation = QuotationTestDataBuilder.createQuotation(1L, "QT-0001", customer);
        UpdateQuotationRequest request = QuotationTestDataBuilder.createUpdateQuotationRequest();

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        QuotationResponse response = quotationService.updateQuotation(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("TOWER");
        assertThat(response.getMinHours()).isEqualTo(new BigDecimal("10"));
        assertThat(response.getMinRate()).isEqualTo(new BigDecimal("6000"));
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentQuotation() {
        // Arrange
        UpdateQuotationRequest request = QuotationTestDataBuilder.createUpdateQuotationRequest();
        when(quotationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quotationService.updateQuotation(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quotation not found");
    }

    @Test
    void shouldDeleteQuotation() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = QuotationTestDataBuilder.createQuotation(1L, "QT-0001", customer);
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        quotationService.deleteQuotation(1L);

        // Assert
        assertThat(quotation.isActive()).isFalse();
        verify(quotationRepository).save(quotation);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentQuotation() {
        // Arrange
        when(quotationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quotationService.deleteQuotation(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quotation not found");
    }
}
