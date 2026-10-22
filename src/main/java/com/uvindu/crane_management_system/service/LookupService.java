package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.dto.QuotationLookupResponse;
import com.uvindu.crane_management_system.model.UserRole;
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
        return craneRepository.findActiveLookupOptions();
    }

    public List<LookupResponse> getCustomers() {
        return customerRepository.findActiveLookupOptions();
    }

    public List<QuotationLookupResponse> getQuotations() {
        return quotationRepository.findActiveLookupOptions();
    }

    public List<LookupResponse> getOperators() {
        return operatorRepository.findLookupOptions();
    }

    public List<LookupResponse> getSalesPeople() {
        // Admins also create job cards and are recorded as the salesperson.
        return userRepository.findActiveLookupOptionsByRoles(List.of(UserRole.ADMIN, UserRole.SALES));
    }

    public List<LookupResponse> getMaintenancePeople() {
        // Existing maintenance assignment permits any user, not only the MAINTENANCE role.
        return userRepository.findActiveLookupOptions();
    }
}
