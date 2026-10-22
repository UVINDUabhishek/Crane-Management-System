package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByRegistrationNo(String registrationNo);

}
