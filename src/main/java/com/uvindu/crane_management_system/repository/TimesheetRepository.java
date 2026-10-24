package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    @Query("select t.jobCard.id from Timesheet t where t.id = :id")
    Optional<Long> findJobCardIdById(@Param("id") Long id);

    Optional<Timesheet> findTopByOrderByIdDesc();

    boolean existsByJobCardAndDateOfWork(JobCard jobCard, LocalDate dateOfWork);

    Optional<Timesheet> findByJobCardAndDateOfWork(
            JobCard jobCard,
            LocalDate dateOfWork
    );

    List<Timesheet> findByJobCard(JobCard jobCard);

    @Query("""
        SELECT t
        FROM Timesheet t
        JOIN FETCH t.jobCard j
        JOIN FETCH j.craneId c
        """)
    List<Timesheet> findAllWithJobCardAndCrane();

    @Query("""
            SELECT t
            FROM Timesheet t
            JOIN FETCH t.jobCard j
            JOIN FETCH j.craneId
            WHERE j.status = :status
            AND t.dateOfWork BETWEEN :fromDate AND :toDate
            """)
    List<Timesheet> findWorkWithinPeriod(
            @Param("status")JobCardStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")LocalDate toDate
            );
}
