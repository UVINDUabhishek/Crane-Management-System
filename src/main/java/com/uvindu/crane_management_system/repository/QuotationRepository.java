package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.QuotationLookupResponse;
import com.uvindu.crane_management_system.model.Quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findTopByOrderByIdDesc();

    @Query("""
            select new com.uvindu.crane_management_system.dto.QuotationLookupResponse(
                q.id, q.quotationNumber, q.customerId.id)
            from Quotation q where q.active = true
            order by q.quotationNumber asc, q.id asc
            """)
    List<QuotationLookupResponse> findActiveLookupOptions();
}
