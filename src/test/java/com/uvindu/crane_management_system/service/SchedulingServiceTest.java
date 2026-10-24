package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.repository.CraneRepository;
import com.uvindu.crane_management_system.repository.JobCardRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulingServiceTest extends BaseUnitTest {
    @Mock CraneRepository cranes;
    @Mock JobCardRepository jobs;
    @Mock EntityManager entityManager;
    @InjectMocks SchedulingService scheduling;

    @Test
    void craneReassignmentLocksDistinctIdsInStableOrderAndRefreshesPreloadedState() {
        Crane first = new Crane(), second = new Crane();
        when(cranes.findByIdForUpdate(1L)).thenReturn(Optional.of(first));
        when(cranes.findByIdForUpdate(2L)).thenReturn(Optional.of(second));
        scheduling.lockCranes(2L, 1L, 2L);
        var order = inOrder(cranes, entityManager);
        order.verify(cranes).findByIdForUpdate(1L);
        order.verify(entityManager).refresh(first);
        order.verify(cranes).findByIdForUpdate(2L);
        order.verify(entityManager).refresh(second);
        order.verifyNoMoreInteractions();
    }

    @Test
    void missingCraneIs404BeforeAnyConflictQueries() {
        assertThatThrownBy(() -> scheduling.lockCranes(99L)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(jobs, entityManager);
    }

    @Test
    void pendingOverlapIsAConflictWithStableCodeAndSelfExclusion() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        when(jobs.existsBookingConflict(1L, start, null, 5L, JobCardStatus.PENDING)).thenReturn(true);
        assertThatThrownBy(() -> scheduling.rejectBookingConflict(1L, start, null, 5L))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(error.getCode()).isEqualTo("BOOKING_OVERLAP");
                });
    }

    @Test
    void dateRangeAllowsOneDayAndOpenEndButRequiresStartAndOrderedEnd() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        assertThatCode(() -> SchedulingService.validateDates(start, null)).doesNotThrowAnyException();
        assertThatCode(() -> SchedulingService.validateDates(start, start)).doesNotThrowAnyException();
        assertThatThrownBy(() -> SchedulingService.validateDates(null, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SchedulingService.validateDates(start, start.minusDays(1)))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
