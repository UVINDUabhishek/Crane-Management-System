package com.uvindu.crane_management_system.integration;

import com.uvindu.crane_management_system.integration.support.AbstractIntegrationTest;
import com.uvindu.crane_management_system.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SchedulingIntegrationTest extends AbstractIntegrationTest {
    @Autowired PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @CsvSource({"10,14,409", "9,10,409", "14,20,409", "11,12,409", "9,20,409", "15,20,201", "1,9,201"})
    void pendingReservationsUseInclusiveOverlap(int start, int end, int expected) throws Exception {
        JobCard job = existingJob(10, 14);
        create(job, job.getCraneId().getId(), start, end).andExpect(status().is(expected));
        assertThat(craneRepository.findById(job.getCraneId().getId()).orElseThrow().getStatus())
                .isEqualTo(CraneStatus.AVAILABLE);
        assertThat(jobCardRepository.count()).isEqualTo(expected == 201 ? 2 : 1);
    }

    @ParameterizedTest
    @EnumSource(value = JobCardStatus.class, names = {"COMPLETED", "CANCELED"})
    void terminalJobsReleaseOpenEndedReservationsAndRemainReadable(JobCardStatus state) throws Exception {
        JobCard job = existingJob(10, null);
        job.setStatus(state);
        jobCardRepository.save(job);
        create(job, job.getCraneId().getId(), 11, 14).andExpect(status().isCreated());
        mockMvc.perform(authorized(get("/api/job-card/{id}", job.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state.name()));
    }

    @Test
    void openEndedExistingJobBlocksFutureBookingsButNotEarlierOrOtherCranes() throws Exception {
        JobCard job = existingJob(10, null);
        create(job, job.getCraneId().getId(), 25, 30).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_OVERLAP"));
        create(job, job.getCraneId().getId(), 1, 9).andExpect(status().isCreated());
        create(job, anotherCrane().getId(), 25, 30).andExpect(status().isCreated());
    }

    @Test
    void updateExcludesItselfAndRejectsConflictsAtomicallyOnBothCranes() throws Exception {
        JobCard job = existingJob(10, 14);
        update(job, job.getCraneId().getId(), 10, 14).andExpect(status().isOk());
        create(job, job.getCraneId().getId(), 20, 25).andExpect(status().isCreated());
        update(job, job.getCraneId().getId(), 14, 20).andExpect(status().isConflict());
        Crane other = anotherCrane();
        create(job, other.getId(), 10, 14).andExpect(status().isCreated());
        update(job, other.getId(), 10, 14).andExpect(status().isConflict());
        JobCard unchanged = jobCardRepository.findById(job.getId()).orElseThrow();
        assertThat(unchanged.getCraneId().getId()).isEqualTo(job.getCraneId().getId());
        assertThat(unchanged.getStartDate()).isEqualTo(date(10));
        assertThat(unchanged.getEndDate()).isEqualTo(date(14));
        update(job, other.getId(), 15, 19).andExpect(status().isOk()).andExpect(jsonPath("$.craneId").value(other.getId()));
        create(job, job.getCraneId().getId(), 10, 14).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({"10,14,10,14,true", "10,,25,30,true", "10,14,1,,true", "10,,1,,true",
            "10,14,15,20,false", "10,14,1,9,false", "10,,1,9,false", "1,9,10,,false"})
    void databaseOverlapHandlesAllBoundedAndOpenRanges(Integer jobStart, Integer jobEnd,
            Integer maintenanceStart, Integer maintenanceEnd, boolean conflict) throws Exception {
        JobCard job = existingJob(jobStart, jobEnd);
        // Maintenance creation uses the same crane lock and the same null-safe booking predicate.
        mockMvc.perform(json(authorized(post("/api/maintenance")), maintenanceBody(job.getCraneId().getId(),
                        maintenanceStart, maintenanceEnd)))
                .andExpect(status().is(conflict ? 409 : 201));
        // Check the reverse predicate independently on PostgreSQL, including null parameters.
        seedMaintenance(job.getCraneId(), maintenanceStart, maintenanceEnd, MaintenanceStatus.SCHEDULED);
        assertThat(maintenanceRepository.existsMaintenanceConflict(job.getCraneId().getId(), date(jobStart), date(jobEnd),
                List.of(MaintenanceStatus.SCHEDULED, MaintenanceStatus.IN_PROGRESS))).isEqualTo(conflict);
        assertThat(jobCardRepository.existsBookingConflict(job.getCraneId().getId(), date(maintenanceStart),
                date(maintenanceEnd), null, JobCardStatus.PENDING)).isEqualTo(conflict);
    }

    @ParameterizedTest
    @EnumSource(MaintenanceStatus.class)
    void onlyActiveMaintenanceBlocksNewJobsEvenWithNullEnd(MaintenanceStatus state) throws Exception {
        JobCard job = existingJob(1, 5);
        seedMaintenance(job.getCraneId(), 10, null, state);
        boolean active = state == MaintenanceStatus.SCHEDULED || state == MaintenanceStatus.IN_PROGRESS;
        create(job, job.getCraneId().getId(), 20, 25).andExpect(status().is(active ? 409 : 201));
    }

    @Test
    void maintenanceUpdateCannotIntroduceAConflictAndDifferentCraneIsAllowed() throws Exception {
        JobCard job = existingJob(10, 14);
        Maintenance maintenance = seedMaintenance(job.getCraneId(), 20, 25, MaintenanceStatus.SCHEDULED);
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenance.getId())),
                maintenanceBody(job.getCraneId().getId(), 14, null))).andExpect(status().isConflict());
        assertThat(maintenanceRepository.findById(maintenance.getId()).orElseThrow().getMaintenanceStartDate())
                .isEqualTo(date(20));
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenance.getId())),
                maintenanceBody(anotherCrane().getId(), 10, null))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"complete", "cancel"})
    void endingOneMaintenancePreservesOtherInProgressWorkWithoutInventingDates(String action) throws Exception {
        Crane crane = fixtures.fixtureCrane();
        Maintenance one = seedMaintenance(crane, 10, null, MaintenanceStatus.SCHEDULED);
        Maintenance two = seedMaintenance(crane, 12, null, MaintenanceStatus.SCHEDULED);
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", one.getId()))).andExpect(status().isOk());
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", two.getId()))).andExpect(status().isOk());
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/" + action, one.getId()))).andExpect(status().isOk());
        assertThat(craneRepository.findById(crane.getId()).orElseThrow().getStatus()).isEqualTo(CraneStatus.MAINTENANCE);
        assertThat(maintenanceRepository.findById(one.getId()).orElseThrow().getMaintenanceEndDate()).isNull();
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/" + action, two.getId()))).andExpect(status().isOk());
        assertThat(craneRepository.findById(crane.getId()).orElseThrow().getStatus()).isEqualTo(CraneStatus.AVAILABLE);
    }

    @Test
    void movingInProgressMaintenanceReconcilesBothCranes() throws Exception {
        Crane original = fixtures.fixtureCrane();
        Crane other = anotherCrane();
        Maintenance maintenance = seedMaintenance(original, 10, null, MaintenanceStatus.SCHEDULED);
        mockMvc.perform(authorized(patch("/api/maintenance/{id}/start", maintenance.getId()))).andExpect(status().isOk());
        mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenance.getId())),
                maintenanceBody(other.getId(), 10, null))).andExpect(status().isOk());
        assertThat(craneRepository.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(CraneStatus.AVAILABLE);
        assertThat(craneRepository.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(CraneStatus.MAINTENANCE);
    }

    @ParameterizedTest
    @CsvSource({"create", "update"})
    void rejectsReversedJobAndMaintenanceDates(String operation) throws Exception {
        JobCard job = existingJob(10, 14);
        (operation.equals("create") ? create(job, job.getCraneId().getId(), 20, 15)
                : update(job, job.getCraneId().getId(), 20, 15)).andExpect(status().isBadRequest());
        Maintenance maintenance = seedMaintenance(job.getCraneId(), 20, 25, MaintenanceStatus.SCHEDULED);
        mockMvc.perform(json(authorized(operation.equals("create") ? post("/api/maintenance")
                        : put("/api/maintenance/{id}", maintenance.getId())), maintenanceBody(job.getCraneId().getId(), 20, 15)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simultaneousBookingsCannotBothSucceed() throws Exception {
        JobCard job = existingJob(1, 5);
        List<Integer> statuses = raceWithCraneLocked(job.getCraneId().getId(),
                () -> create(job, job.getCraneId().getId(), 10, 14).andReturn().getResponse().getStatus(),
                () -> create(job, job.getCraneId().getId(), 12, 16).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jobCardRepository.count()).isEqualTo(2);
    }

    @Test
    void simultaneousMaintenanceAndJobCreationCannotBothSucceed() throws Exception {
        JobCard job = existingJob(1, 5);
        List<Integer> statuses = raceWithCraneLocked(job.getCraneId().getId(),
                () -> create(job, job.getCraneId().getId(), 10, 14).andReturn().getResponse().getStatus(),
                () -> mockMvc.perform(json(authorized(post("/api/maintenance")), maintenanceBody(job.getCraneId().getId(), 12, null)))
                        .andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jobCardRepository.count() + maintenanceRepository.count()).isEqualTo(2);
    }

    @Test
    void simultaneousUpdatesCannotReserveTheSameCranePeriod() throws Exception {
        JobCard first = existingJob(1, 5);
        create(first, first.getCraneId().getId(), 10, 14).andExpect(status().isCreated());
        JobCard second = jobCardRepository.findTopByOrderByIdDesc().orElseThrow();
        List<Integer> statuses = raceWithCraneLocked(first.getCraneId().getId(),
                () -> update(first, first.getCraneId().getId(), 20, 25).andReturn().getResponse().getStatus(),
                () -> update(second, second.getCraneId().getId(), 20, 25).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jobCardRepository.findAll().stream().filter(job -> job.getStartDate().equals(date(20))).count()).isEqualTo(1);
    }

    @Test
    void oppositeCraneReassignmentsUseStableLockOrder() throws Exception {
        JobCard first = existingJob(1, 5);
        Crane other = anotherCrane();
        create(first, other.getId(), 10, 14).andExpect(status().isCreated());
        JobCard second = jobCardRepository.findTopByOrderByIdDesc().orElseThrow();
        List<Integer> statuses = raceWithCraneLocked(first.getCraneId().getId(),
                () -> update(first, other.getId(), 1, 5).andReturn().getResponse().getStatus(),
                () -> update(second, first.getCraneId().getId(), 10, 14).andReturn().getResponse().getStatus());
        assertThat(statuses).containsExactly(200, 200);
        assertThat(jobCardRepository.findById(first.getId()).orElseThrow().getCraneId().getId()).isEqualTo(other.getId());
        assertThat(jobCardRepository.findById(second.getId()).orElseThrow().getCraneId().getId()).isEqualTo(first.getCraneId().getId());
    }

    @Test
    void maintenanceUpdateAndNewJobCannotIntroduceConflictingReservations() throws Exception {
        JobCard job = existingJob(1, 5);
        Maintenance maintenance = seedMaintenance(job.getCraneId(), 20, 25, MaintenanceStatus.SCHEDULED);
        List<Integer> statuses = raceWithCraneLocked(job.getCraneId().getId(),
                () -> create(job, job.getCraneId().getId(), 10, 14).andReturn().getResponse().getStatus(),
                () -> mockMvc.perform(json(authorized(put("/api/maintenance/{id}", maintenance.getId())),
                        maintenanceBody(job.getCraneId().getId(), 10, 14))).andReturn().getResponse().getStatus());
        assertThat(statuses).contains(409);
        assertThat(statuses.getFirst() == 201 || statuses.get(1) == 200).isTrue();
    }

    private List<Integer> raceWithCraneLocked(Long id, Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> requests = new TransactionTemplate(transactionManager).execute(transaction -> {
                craneRepository.findByIdForUpdate(id).orElseThrow();
                List<Future<Integer>> futures = List.of(executor.submit(first), executor.submit(second));
                // Both HTTP transactions must actually be waiting for the same DB row before releasing it.
                await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and query like '%from cranes%'",
                        Integer.class)).isEqualTo(2));
                return futures;
            });
            return List.of(requests.get(0).get(20, TimeUnit.SECONDS), requests.get(1).get(20, TimeUnit.SECONDS));
        }
    }

    private JobCard existingJob(Integer start, Integer end) throws Exception {
        JobCard job = fixtures.fixtureJobCard();
        job.setStartDate(date(start));
        job.setEndDate(date(end));
        return jobCardRepository.save(job);
    }

    private Crane anotherCrane() {
        Crane crane = new Crane();
        crane.setCraneNumber("CR-SECOND"); crane.setSerialNumber("SECOND"); crane.setType("001");
        crane.setStatus(CraneStatus.AVAILABLE);
        return craneRepository.save(crane);
    }

    private Maintenance seedMaintenance(Crane crane, Integer start, Integer end, MaintenanceStatus status) {
        Maintenance maintenance = new Maintenance();
        maintenance.setMaintenanceNumber("MT-%04d".formatted(maintenanceRepository.count() + 1));
        maintenance.setCrane(crane); maintenance.setMaintenanceDate(date(start));
        maintenance.setMaintenanceStartDate(date(start)); maintenance.setMaintenanceEndDate(date(end));
        maintenance.setMaintenanceType(MaintenanceType.PREVENTIVE); maintenance.setStatus(status);
        return maintenanceRepository.save(maintenance);
    }

    private ResultActions create(JobCard job, Long craneId, Integer start, Integer end) throws Exception {
        return mockMvc.perform(json(authorized(post("/api/job-card")), jobBody(job, craneId, start, end)));
    }
    private ResultActions update(JobCard job, Long craneId, Integer start, Integer end) throws Exception {
        return mockMvc.perform(json(authorized(put("/api/job-card/{id}", job.getId())), jobBody(job, craneId, start, end)));
    }
    private String jobBody(JobCard job, Long craneId, Integer start, Integer end) {
        return """
                {"customerId":%d,"quotationId":%d,"craneId":%d,"startDate":%s,"endDate":%s,
                 "location":"Colombo","description":"Scheduling test"}
                """.formatted(job.getCustomerId().getId(), job.getQuotationId().getId(), craneId, jsonDate(start), jsonDate(end));
    }
    private String maintenanceBody(Long craneId, Integer start, Integer end) {
        return """
                {"craneId":%d,"maintenanceStartDate":%s,"maintenanceEndDate":%s,
                 "maintenanceType":"PREVENTIVE","description":"Scheduling test"}
                """.formatted(craneId, jsonDate(start), jsonDate(end));
    }
    private LocalDate date(Integer day) { return day == null ? null : LocalDate.of(2026, 11, day); }
    private String jsonDate(Integer day) { return day == null ? "null" : "\"" + date(day) + "\""; }
}
