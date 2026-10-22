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
