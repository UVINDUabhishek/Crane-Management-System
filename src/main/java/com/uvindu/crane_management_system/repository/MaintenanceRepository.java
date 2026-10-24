package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    @Query(value = "select * from maintenance where id = :id for update", nativeQuery = true)
    Optional<Maintenance> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    boolean existsByCrane_IdAndStatus(Long craneId, MaintenanceStatus status);

    Optional<Maintenance> findTopByOrderByIdDesc();

    @Query("""
        SELECT COUNT(m) > 0
        FROM Maintenance m
        WHERE m.crane.id = :craneId
        AND m.status IN :statuses
        AND (cast(:jobEndDate as date) IS NULL OR m.maintenanceStartDate <= :jobEndDate)
        AND (m.maintenanceEndDate IS NULL OR m.maintenanceEndDate >= :jobStartDate)
        """)
    boolean existsMaintenanceConflict(
            @Param("craneId") Long craneId,
            @Param("jobStartDate") LocalDate jobStartDate,
            @Param("jobEndDate") LocalDate jobEndDate,
            @Param("statuses") List<MaintenanceStatus> statuses
    );

    // Find maintenance periods overlapping the reporting period
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.crane
        WHERE m.status IN :statuses
        AND m.maintenanceStartDate <= :toDate
        AND (m.maintenanceEndDate IS NULL
             OR m.maintenanceEndDate >= :fromDate)
        """)
    List<Maintenance> findMaintenanceOverlapping(
            @Param("statuses") List<MaintenanceStatus> statuses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
