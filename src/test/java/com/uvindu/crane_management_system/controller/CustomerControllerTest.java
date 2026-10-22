package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CreateCustomerRequest;
import com.uvindu.crane_management_system.dto.CustomerResponse;
import com.uvindu.crane_management_system.dto.UpdateCustomerRequest;
import com.uvindu.crane_management_system.service.CustomerService;
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
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;


    // ---------------------------------------------------------
    // Create Customer
    // ---------------------------------------------------------

    @Test
    void shouldCreateCustomer() {

        // Arrange
        CreateCustomerRequest request =
                new CreateCustomerRequest();

        CustomerResponse customerResponse =
                new CustomerResponse();

        when(customerService.createCustomer(request))
                .thenReturn(customerResponse);

        // Act
        ResponseEntity<CustomerResponse> response =
                customerController.createCustomer(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(customerResponse);

        verify(customerService).createCustomer(request);
    }


    // ---------------------------------------------------------
    // Get All Customers
    // ---------------------------------------------------------

    @Test
    void shouldGetAllCustomers() {

        // Arrange
        CustomerResponse customer1 =
                new CustomerResponse();

        CustomerResponse customer2 =
                new CustomerResponse();

        List<CustomerResponse> customers =
                List.of(customer1, customer2);

        when(customerService.getAllCustomers())
                .thenReturn(customers);

        // Act
        ResponseEntity<List<CustomerResponse>> response =
                customerController.getAllCustomers();

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(customers);
        assertThat(response.getBody()).hasSize(2);

        verify(customerService).getAllCustomers();
    }


    // ---------------------------------------------------------
    // Get Customer By ID
    // ---------------------------------------------------------

    @Test
    void shouldGetCustomerById() {

        // Arrange
        Long customerId = 1L;

        CustomerResponse customerResponse =
                new CustomerResponse();

        when(customerService.getCustomerById(customerId))
                .thenReturn(customerResponse);

        // Act
        ResponseEntity<CustomerResponse> response =
                customerController.getCustomerById(customerId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(customerResponse);

        verify(customerService).getCustomerById(customerId);
    }


    // ---------------------------------------------------------
    // Update Customer
    // ---------------------------------------------------------

    @Test
    void shouldUpdateCustomer() {

        // Arrange
        Long customerId = 1L;

        UpdateCustomerRequest request =
                new UpdateCustomerRequest();

        CustomerResponse updatedCustomer =
                new CustomerResponse();

        when(customerService.updateCustomer(customerId, request))
                .thenReturn(updatedCustomer);

        // Act
        ResponseEntity<CustomerResponse> response =
                customerController.updateCustomer(customerId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(updatedCustomer);

        verify(customerService).updateCustomer(
                customerId,
                request
        );
    }


    // ---------------------------------------------------------
    // Delete Customer
    // ---------------------------------------------------------

    @Test
    void shouldDeleteCustomer() {

        // Arrange
        Long customerId = 1L;

        // Act
        ResponseEntity<Void> response =
                customerController.deleteCustomer(customerId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(customerService).deleteCustomer(customerId);
    }
}