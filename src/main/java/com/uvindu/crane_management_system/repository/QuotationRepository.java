package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findTopByOrderByIdDesc();
}
