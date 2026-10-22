package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.MaintenanceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MaintenanceRepositoryTest {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private CraneRepository craneRepository;

    private Crane crane;
    private Crane secondCrane;

    @BeforeEach
    void setUp() {
        crane = createCrane(
                "CR012M901",
                "12T",
                "SN-MAINT-001"
        );

        secondCrane = createCrane(
                "CR060M901",
                "60T",
                "SN-MAINT-002"
        );
    }

    @Test
    void shouldFindLatestMaintenance() {
        Maintenance firstMaintenance = createMaintenance(
                "MT-TEST-0001",
                crane,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                MaintenanceStatus.COMPLETED
        );

        Maintenance latestMaintenance = createMaintenance(
                "MT-TEST-0002",
                secondCrane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.save(firstMaintenance);
        maintenanceRepository.save(latestMaintenance);

        Optional<Maintenance> result =
                maintenanceRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getId())
                .isEqualTo(latestMaintenance.getId());
    }

    @Test
    void shouldReturnEmptyWhenNoMaintenanceExists() {
        Optional<Maintenance> result =
                maintenanceRepository.findTopByOrderByIdDesc();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenMaintenanceConflictsWithJobPeriod() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0011",
                crane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 13),
                List.of(MaintenanceStatus.SCHEDULED)
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMaintenanceDoesNotConflictWithJobPeriod() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0021",
                crane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 18),
                List.of(MaintenanceStatus.SCHEDULED)
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenMaintenanceBelongsToDifferentCrane() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0031",
                secondCrane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 13),
                List.of(MaintenanceStatus.SCHEDULED)
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenMaintenanceStatusIsNotIncluded() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0041",
                crane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.COMPLETED
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 13),
                List.of(MaintenanceStatus.SCHEDULED)
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldDetectConflictWhenJobStartsOnMaintenanceEndDate() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0051",
                crane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.IN_PROGRESS
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 16),
                List.of(MaintenanceStatus.IN_PROGRESS)
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldDetectConflictWhenJobEndsOnMaintenanceStartDate() {
        Maintenance maintenance = createMaintenance(
                "MT-TEST-0061",
                crane,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.save(maintenance);

        boolean result = maintenanceRepository.existsMaintenanceConflict(
                crane.getId(),
                LocalDate.of(2026, 9, 9),
                LocalDate.of(2026, 9, 10),
                List.of(MaintenanceStatus.SCHEDULED)
        );

        assertThat(result).isTrue();
    }


    @Test
    void shouldFindMaintenanceOverlappingDateRange() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance startsBefore = createMaintenance(
                "MT-IDLE-001",
                crane,
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 12),
                MaintenanceStatus.SCHEDULED
        );

        Maintenance withinRange = createMaintenance(
                "MT-IDLE-002",
                secondCrane,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 16),
                MaintenanceStatus.IN_PROGRESS
        );

        Maintenance endsAfter = createMaintenance(
                "MT-IDLE-003",
                crane,
                LocalDate.of(2026, 10, 18),
                LocalDate.of(2026, 10, 25),
                MaintenanceStatus.COMPLETED
        );

        maintenanceRepository.saveAllAndFlush(
                List.of(startsBefore, withinRange, endsAfter)
        );

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                        ),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(3);

        assertThat(result)
                .extracting(Maintenance::getMaintenanceNumber)
                .containsExactlyInAnyOrder(
                        "MT-IDLE-001",
                        "MT-IDLE-002",
                        "MT-IDLE-003"
                );
    }


    @Test
    void shouldExcludeMaintenanceOutsideDateRange() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance beforeRange = createMaintenance(
                "MT-IDLE-004",
                crane,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 9),
                MaintenanceStatus.COMPLETED
        );

        Maintenance afterRange = createMaintenance(
                "MT-IDLE-005",
                secondCrane,
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 25),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.saveAllAndFlush(
                List.of(beforeRange, afterRange)
        );

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                        ),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).isEmpty();
    }


    @Test
    void shouldExcludeCanceledMaintenanceFromIdleStatistics() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance scheduled = createMaintenance(
                "MT-IDLE-006",
                crane,
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 14),
                MaintenanceStatus.SCHEDULED
        );

        Maintenance canceled = createMaintenance(
                "MT-IDLE-007",
                secondCrane,
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 17),
                MaintenanceStatus.CANCELED
        );

        maintenanceRepository.saveAllAndFlush(
                List.of(scheduled, canceled)
        );

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                        ),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        assertThat(result.getFirst().getMaintenanceNumber())
                .isEqualTo("MT-IDLE-006");

        assertThat(result)
                .extracting(Maintenance::getStatus)
                .doesNotContain(MaintenanceStatus.CANCELED);
    }


    @Test
    void shouldIncludeMaintenanceOnDateRangeBoundaries() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance endsOnFirstDay = createMaintenance(
                "MT-IDLE-008",
                crane,
                LocalDate.of(2026, 10, 8),
                fromDate,
                MaintenanceStatus.COMPLETED
        );

        Maintenance startsOnLastDay = createMaintenance(
                "MT-IDLE-009",
                secondCrane,
                toDate,
                LocalDate.of(2026, 10, 23),
                MaintenanceStatus.SCHEDULED
        );

        maintenanceRepository.saveAllAndFlush(
                List.of(endsOnFirstDay, startsOnLastDay)
        );

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS,
                                MaintenanceStatus.COMPLETED
                        ),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Maintenance::getMaintenanceNumber)
                .containsExactlyInAnyOrder(
                        "MT-IDLE-008",
                        "MT-IDLE-009"
                );
    }


    @Test
    void shouldFindMaintenanceWithNullEndDate() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance openEnded = createMaintenance(
                "MT-IDLE-010",
                crane,
                LocalDate.of(2026, 10, 15),
                null,
                MaintenanceStatus.IN_PROGRESS
        );

        maintenanceRepository.saveAndFlush(openEnded);

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(MaintenanceStatus.IN_PROGRESS),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        Maintenance maintenance = result.getFirst();

        assertThat(maintenance.getMaintenanceNumber())
                .isEqualTo("MT-IDLE-010");

        assertThat(maintenance.getMaintenanceStartDate())
                .isEqualTo(LocalDate.of(2026, 10, 15));

        assertThat(maintenance.getMaintenanceEndDate())
                .isNull();
    }


    @Test
    void shouldReturnMaintenanceForIncludedStatusesWithCorrectCrane() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        Maintenance scheduled = createMaintenance(
                "MT-IDLE-011",
                crane,
                LocalDate.of(2026, 10, 11),
                LocalDate.of(2026, 10, 13),
                MaintenanceStatus.SCHEDULED
        );

        Maintenance inProgress = createMaintenance(
                "MT-IDLE-012",
                secondCrane,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 16),
                MaintenanceStatus.IN_PROGRESS
        );

        Maintenance completed = createMaintenance(
                "MT-IDLE-013",
                crane,
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 19),
                MaintenanceStatus.COMPLETED
        );

        maintenanceRepository.saveAllAndFlush(
                List.of(scheduled, inProgress, completed)
        );

        // Act
        List<Maintenance> result =
                maintenanceRepository.findMaintenanceOverlapping(
                        List.of(
                                MaintenanceStatus.SCHEDULED,
                                MaintenanceStatus.IN_PROGRESS
                        ),
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Maintenance::getMaintenanceNumber)
                .containsExactlyInAnyOrder(
                        "MT-IDLE-011",
                        "MT-IDLE-012"
                );

        Maintenance scheduledResult = result.stream()
                .filter(m -> m.getMaintenanceNumber().equals("MT-IDLE-011"))
                .findFirst()
                .orElseThrow();

        Maintenance inProgressResult = result.stream()
                .filter(m -> m.getMaintenanceNumber().equals("MT-IDLE-012"))
                .findFirst()
                .orElseThrow();

        assertThat(scheduledResult.getCrane().getId())
                .isEqualTo(crane.getId());

        assertThat(inProgressResult.getCrane().getId())
                .isEqualTo(secondCrane.getId());

        assertThat(result)
                .extracting(Maintenance::getStatus)
                .containsExactlyInAnyOrder(
                        MaintenanceStatus.SCHEDULED,
                        MaintenanceStatus.IN_PROGRESS
                );
    }


    private Crane createCrane(
            String craneNumber,
            String type,
            String serialNumber
    ) {
        Crane crane = new Crane();

        crane.setCraneNumber(craneNumber);
        crane.setType(type);
        crane.setCapacity(12.0);
        crane.setManufacturer("Test Manufacturer");
        crane.setSerialNumber(serialNumber);
        crane.setYear(2025);
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(true);
        crane.setLocation("Test Yard");

        return craneRepository.save(crane);
    }

    private Maintenance createMaintenance(
            String maintenanceNumber,
            Crane crane,
            LocalDate startDate,
            LocalDate endDate,
            MaintenanceStatus status
    ) {
        Maintenance maintenance = new Maintenance();

        maintenance.setMaintenanceNumber(maintenanceNumber);
        maintenance.setCrane(crane);
        maintenance.setMaintenanceDate(startDate);
        maintenance.setMaintenanceStartDate(startDate);
        maintenance.setMaintenanceEndDate(endDate);
        maintenance.setMaintenanceType(MaintenanceType.SERVICE);
        maintenance.setDescription("Test maintenance");
        maintenance.setStatus(status);

        return maintenance;
    }
}