package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;

/** Shared locking protocol for job and maintenance mutations. */
@Service
public class SchedulingService {
    private final CraneRepository cranes;
    private final JobCardRepository jobs;
    private final EntityManager entityManager;

    public SchedulingService(CraneRepository cranes, JobCardRepository jobs, EntityManager entityManager) {
        this.cranes = cranes;
        this.jobs = jobs;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockCranes(Long... ids) {
        // Existing job/maintenance row first, then distinct crane IDs in ascending order.
        Arrays.stream(ids).distinct().sorted().forEach(id -> {
            Crane crane = cranes.findByIdForUpdate(id)
                    .orElseThrow(() -> BusinessException.notFound("Crane not found"));
            // An eager association may have loaded this crane before waiting for its lock.
            entityManager.refresh(crane);
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void rejectBookingConflict(Long craneId, LocalDate start, LocalDate end, Long excludeId) {
        if (jobs.existsBookingConflict(craneId, start, end, excludeId, JobCardStatus.PENDING)) {
            throw new BusinessException(HttpStatus.CONFLICT, "BOOKING_OVERLAP",
                    "Crane has a pending job during the selected period");
        }
    }

    public static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || (end != null && end.isBefore(start))) {
            throw BusinessException.invalid("Start date is required and end date cannot be before start date");
        }
    }
}
