package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.model.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByRegistrationNo(String registrationNo);

    @Query("""
            select new com.uvindu.crane_management_system.dto.LookupResponse(c.id, c.companyName)
            from Customer c where c.active = true
            order by c.companyName asc, c.id asc
            """)
    List<LookupResponse> findActiveLookupOptions();
}
