package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateCustomerRequest;
import com.uvindu.crane_management_system.dto.CustomerResponse;
import com.uvindu.crane_management_system.dto.UpdateCustomerRequest;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.repository.CustomerRepository;
import com.uvindu.crane_management_system.util.CustomerTestDataBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

class CustomerServiceTest extends BaseUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldCreateCustomerSuccessfully() {
        // Arrange
        CreateCustomerRequest request = CustomerTestDataBuilder.createCustomerRequest();

        when(customerRepository.existsByRegistrationNo("REG-001")).thenReturn(false);
        Customer savedCustomer = CustomerTestDataBuilder.createCustomer(1L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        // Act
        CustomerResponse response = customerService.createCustomer(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("ABC Construction");
        assertThat(response.getRegistrationNo()).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingCustomerWithDuplicateRegistrationNo() {
        // Arrange
        CreateCustomerRequest request = CustomerTestDataBuilder.createCustomerRequest();
        when(customerRepository.existsByRegistrationNo("REG-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer already exists");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void shouldGetAllCustomers() {
        // Arrange
        Customer customer1 = CustomerTestDataBuilder.createCustomer(1L);
        Customer customer2 = CustomerTestDataBuilder.createCustomer(2L);
        when(customerRepository.findAll()).thenReturn(Arrays.asList(customer1, customer2));

        // Act
        List<CustomerResponse> responses = customerService.getAllCustomers();

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        verify(customerRepository).findAll();
    }

    @Test
    void shouldGetCustomerById() {
        // Arrange
        Customer customer = CustomerTestDataBuilder.createCustomer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse response = customerService.getCustomerById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("ABC Construction");
        verify(customerRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentCustomer() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void shouldUpdateCustomer() {
        // Arrange
        Customer existingCustomer = CustomerTestDataBuilder.createCustomer(1L);
        UpdateCustomerRequest request = CustomerTestDataBuilder.createUpdateCustomerRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CustomerResponse response = customerService.updateCustomer(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCompanyName()).isEqualTo("Updated Construction");
        assertThat(response.getCompanyAddress()).isEqualTo("Updated Address");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCustomer() {
        // Arrange
        UpdateCustomerRequest request = CustomerTestDataBuilder.createUpdateCustomerRequest();
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.updateCustomer(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer not found");
    }

    @Test
    void shouldDeleteCustomer() {
        // Arrange
        Customer customer = CustomerTestDataBuilder.createCustomer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        customerService.deleteCustomer(1L);

        // Assert
        assertThat(customer.isActive()).isFalse();
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer not found");
    }
}
