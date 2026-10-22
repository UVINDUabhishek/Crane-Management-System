package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.model.Operator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    @Query("""
            select new com.uvindu.crane_management_system.dto.LookupResponse(o.id, o.operatorName)
            from Operator o
            order by o.operatorName asc, o.id asc
            """)
    List<LookupResponse> findLookupOptions();
}
