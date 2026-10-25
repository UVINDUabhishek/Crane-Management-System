package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.dto.CreateCustomerRequest;
import com.uvindu.crane_management_system.dto.CustomerResponse;
import com.uvindu.crane_management_system.dto.UpdateCustomerRequest;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Create Customer
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        if(customerRepository.existsByRegistrationNo(request.getRegistrationNo())) {
            throw BusinessException.conflict("Customer already exists");
        }

        Customer customer = new Customer();

        customer.setCompanyName(request.getCompanyName());
        customer.setRegistrationNo(request.getRegistrationNo());
        customer.setVatNo(request.getVatNo());
        customer.setCompanyAddress(request.getCompanyAddress());
        customer.setBillAddress(request.getBillAddress());
        customer.setContactPerson(request.getContactPerson());
        customer.setContactNo(request.getContactNo());
        customer.setCustomerEmail(request.getCustomerEmail());

        Customer savedCustomer = customerRepository.save(customer);

        return mapToResponse(savedCustomer);
    }

    // Get All Customers
    public List<CustomerResponse> getAllCustomers() {

        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

    }

    // Get Customer By ID
    public CustomerResponse getCustomerById(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        BusinessException.notFound("Customer not found"));

        return mapToResponse(customer);

    }

    // Update Customer
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        BusinessException.notFound("Customer not found"));

        customer.setCompanyName(request.getCompanyName());
        customer.setRegistrationNo(request.getRegistrationNo());
        customer.setVatNo(request.getVatNo());
        customer.setCompanyAddress(request.getCompanyAddress());
        customer.setBillAddress(request.getBillAddress());
        customer.setContactPerson(request.getContactPerson());
        customer.setContactNo(request.getContactNo());
        customer.setCustomerEmail(request.getCustomerEmail());

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);

    }

    // Delete Customer
    public void deleteCustomer(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        BusinessException.notFound("Customer not found"));

        customer.setActive(false);

        customerRepository.save(customer);
    }

    @Transactional
    public CustomerResponse updateActivation(Long id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Customer not found"));
        customer.setActive(active);
        return mapToResponse(customerRepository.save(customer));
    }

    // Mapping method
    private CustomerResponse mapToResponse(Customer customer) {

        CustomerResponse response = new CustomerResponse();

        response.setId(customer.getId());
        response.setActive(customer.isActive());
        response.setCompanyName(customer.getCompanyName());
        response.setRegistrationNo(customer.getRegistrationNo());
        response.setVatNo(customer.getVatNo());
        response.setCompanyAddress(customer.getCompanyAddress());
        response.setBillAddress(customer.getBillAddress());
        response.setContactPerson(customer.getContactPerson());
        response.setContactNo(customer.getContactNo());
        response.setCustomerEmail(customer.getCustomerEmail());

        return response;
    }

}
