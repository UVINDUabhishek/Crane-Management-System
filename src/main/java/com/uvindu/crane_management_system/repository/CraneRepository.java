package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.dto.CraneLookupResponse;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CraneRepository extends JpaRepository<Crane, Long> {

    @Query(value = "select * from cranes where id = :id for update", nativeQuery = true)
    Optional<Crane> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    // Check for duplicates before creating one
    boolean existsBySerialNumber(String serialNumber);

    // Find the highest crane number for a particular type.
    Optional<Crane> findTopByTypeOrderByCraneNumberDesc(String type);

    // Dashboard counts
    long countByStatus(CraneStatus status);

    @Query("""
            select new com.uvindu.crane_management_system.dto.CraneLookupResponse(c.id, c.craneNumber, c.type)
            from Crane c where c.active = true
            order by c.craneNumber asc, c.id asc
            """)
    List<CraneLookupResponse> findActiveLookupOptions();
}
