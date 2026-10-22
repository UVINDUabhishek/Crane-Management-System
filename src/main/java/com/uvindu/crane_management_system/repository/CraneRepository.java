package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.LookupResponse;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CraneRepository extends JpaRepository<Crane, Long> {

    // Check for duplicates before creating one
    boolean existsBySerialNumber(String serialNumber);

    // Find the highest crane number for a particular type.
    Optional<Crane> findTopByTypeOrderByCraneNumberDesc(String type);

    // Dashboard counts
    long countByStatus(CraneStatus status);

    @Query("""
            select new com.uvindu.crane_management_system.dto.LookupResponse(c.id, c.craneNumber)
            from Crane c where c.active = true
            order by c.craneNumber asc, c.id asc
            """)
    List<LookupResponse> findActiveLookupOptions();
}
