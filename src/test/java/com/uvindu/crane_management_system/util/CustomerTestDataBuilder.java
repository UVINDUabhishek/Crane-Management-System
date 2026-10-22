package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateCustomerRequest;
import com.uvindu.crane_management_system.dto.UpdateCustomerRequest;
import com.uvindu.crane_management_system.model.Customer;

public class CustomerTestDataBuilder {

    public static Customer createCustomer(Long id) {

        Customer customer = new Customer();
        customer.setId(id);
        customer.setCompanyName("ABC Construction");
        customer.setRegistrationNo("REG-" + String.format("%03d", id));
        customer.setVatNo("VAT-" + String.format("%03d", id));
        customer.setCompanyAddress("Colombo");
        customer.setBillAddress("Colombo");
        customer.setContactPerson("John Silva");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("john@abc.com");
        customer.setActive(true);

        return customer;
    }

    public static CreateCustomerRequest createCustomerRequest() {

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("ABC Construction");
        request.setRegistrationNo("REG-001");
        request.setVatNo("VAT-001");
        request.setCompanyAddress("Colombo");
        request.setBillAddress("Colombo");
        request.setContactPerson("John Silva");
        request.setContactNo("0771234567");
        request.setCustomerEmail("john@abc.com");

        return request;
    }

    public static UpdateCustomerRequest createUpdateCustomerRequest() {

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Updated Construction");
        request.setRegistrationNo("REG-001");
        request.setVatNo("VAT-001");
        request.setCompanyAddress("Updated Address");
        request.setBillAddress("Updated Bill Address");
        request.setContactPerson("Updated Person");
        request.setContactNo("0777654321");
        request.setCustomerEmail("updated@abc.com");

        return request;
    }
}
