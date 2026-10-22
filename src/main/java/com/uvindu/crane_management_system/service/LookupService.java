package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.dto.QuotationLookupResponse;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupService {
    private final CraneRepository craneRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;

    public List<LookupResponse> getCranes() {
        return craneRepository.findAll().stream().filter(Crane::isActive)
                .map(crane -> new LookupResponse(crane.getId(), crane.getCraneNumber())).toList();
    }

    public List<LookupResponse> getCustomers() {
        return customerRepository.findAll().stream().filter(Customer::isActive)
                .map(customer -> new LookupResponse(customer.getId(), customer.getCompanyName())).toList();
    }

    public List<QuotationLookupResponse> getQuotations() {
        return quotationRepository.findAll().stream().filter(Quotation::isActive)
                .map(quotation -> new QuotationLookupResponse(quotation.getId(),
                        quotation.getQuotationNumber(), quotation.getCustomerId().getId())).toList();
    }

    public List<LookupResponse> getOperators() {
        return operatorRepository.findAll().stream()
                .map(operator -> new LookupResponse(operator.getId(), operator.getOperatorName())).toList();
    }

    public List<LookupResponse> getSalesPeople() {
        // Admins also create job cards and are recorded as the salesperson.
        return userRepository.findAll().stream().filter(User::isActive)
                .filter(user -> user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SALES)
                .map(user -> new LookupResponse(user.getId(), user.getUsername())).toList();
    }

    public List<LookupResponse> getMaintenancePeople() {
        // Existing maintenance assignment permits any user, not only the MAINTENANCE role.
        return userRepository.findAll().stream().filter(User::isActive)
                .map(user -> new LookupResponse(user.getId(), user.getUsername())).toList();
    }
}
