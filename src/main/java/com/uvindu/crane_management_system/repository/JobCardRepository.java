package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    @Query(value = "select * from job_card where id = :id for update", nativeQuery = true)
    Optional<JobCard> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    @Query("""
            select count(j) > 0 from JobCard j
            where j.craneId.id = :craneId and j.status = :status
            and (:excludeId is null or j.id <> :excludeId)
            and (cast(:endDate as date) is null or j.startDate <= :endDate)
            and (j.endDate is null or j.endDate >= :startDate)
            """)
    boolean existsBookingConflict(@Param("craneId") Long craneId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId, @Param("status") JobCardStatus status);

    Optional<JobCard> findTopByOrderByIdDesc();

    List<JobCard> findByJobDateBetween(LocalDate fromDate, LocalDate toDate);

    List<JobCard> findByCustomerId_Id(Long customerId);

    List<JobCard> findByCraneId_Id(Long craneId);

    List<JobCard> findByStatus(JobCardStatus status);

    List<JobCard> findBySalesPersonId_Id(Long salesPersonId);

    // Dashboard counts
    long countByStatus(JobCardStatus status);

    // Get pending reservations overlapping the selected date range
    @Query("""
            SELECT j
            FROM JobCard j
            JOIN FETCH j.craneId
            WHERE j.status = :status
            AND j.startDate <= :toDate
            AND (j.endDate IS NULL OR j.endDate >= :fromDate)
            """)
    List<JobCard> findReservationOverlapping(
            @Param("status") JobCardStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    // Find pending crane reservations overlapping the reporting period
    @Query("""
        SELECT j
        FROM JobCard j
        JOIN FETCH j.craneId
        WHERE j.status = :status
        AND j.startDate <= :toDate
        AND (j.endDate IS NULL OR j.endDate >= :fromDate)
        """)
    List<JobCard> findReservationsOverlapping(
            @Param("status") JobCardStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );


}
