package com.uvindu.crane_management_system.repository;


import com.uvindu.crane_management_system.model.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {
}
