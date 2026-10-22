package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CraneRepositoryTest {

    @Autowired
    private CraneRepository craneRepository;

    // ============================================================
    // existsBySerialNumber
    // ============================================================

    @Test
    void shouldReturnTrueWhenSerialNumberExists() {

        Crane crane = new Crane();

        crane.setCraneNumber("CR012T001");
        crane.setType("12T");
        crane.setCapacity(12);
        crane.setManufacturer("Liebherr");
        crane.setSerialNumber("SN-TEST-001");
        crane.setYear(2021);
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(true);
        crane.setLocation("Main Yard");

        craneRepository.save(crane);

        boolean exists =
                craneRepository.existsBySerialNumber("SN-TEST-001");

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSerialNumberDoesNotExist() {

        boolean exists =
                craneRepository.existsBySerialNumber("SN-NOT-EXIST");

        assertThat(exists).isFalse();
    }

    // ============================================================
    // findTopByTypeOrderByCraneNumberDesc
    // ============================================================

    @Test
    void shouldFindHighestCraneNumberForType() {

        Crane crane1 = new Crane();

        crane1.setCraneNumber("CR012T001");
        crane1.setType("12T");
        crane1.setCapacity(12);
        crane1.setManufacturer("Liebherr");
        crane1.setSerialNumber("SN-TEST-002");
        crane1.setYear(2021);
        crane1.setStatus(CraneStatus.AVAILABLE);
        crane1.setActive(true);
        crane1.setLocation("Main Yard");

        Crane crane2 = new Crane();

        crane2.setCraneNumber("CR012T002");
        crane2.setType("12T");
        crane2.setCapacity(12);
        crane2.setManufacturer("Tadano");
        crane2.setSerialNumber("SN-TEST-003");
        crane2.setYear(2022);
        crane2.setStatus(CraneStatus.AVAILABLE);
        crane2.setActive(true);
        crane2.setLocation("Main Yard");

        craneRepository.save(crane1);
        craneRepository.save(crane2);

        Optional<Crane> result =
                craneRepository.findTopByTypeOrderByCraneNumberDesc("12T");

        assertThat(result).isPresent();
        assertThat(result.get().getCraneNumber())
                .isEqualTo("CR012T002");
    }

    @Test
    void shouldReturnEmptyWhenNoCraneExistsForType() {

        Optional<Crane> result =
                craneRepository.findTopByTypeOrderByCraneNumberDesc("500T");

        assertThat(result).isEmpty();
    }

    // ============================================================
    // countByStatus
    // ============================================================

    @Test
    void shouldCountCraneByStatus() {

        Crane crane1 = new Crane();

        crane1.setCraneNumber("CR012T003");
        crane1.setType("12T");
        crane1.setCapacity(12);
        crane1.setManufacturer("Liebherr");
        crane1.setSerialNumber("SN-TEST-004");
        crane1.setYear(2021);
        crane1.setStatus(CraneStatus.AVAILABLE);
        crane1.setActive(true);
        crane1.setLocation("Main Yard");

        Crane crane2 = new Crane();

        crane2.setCraneNumber("CR120T001");
        crane2.setType("120T");
        crane2.setCapacity(120);
        crane2.setManufacturer("Tadano");
        crane2.setSerialNumber("SN-TEST-005");
        crane2.setYear(2025);
        crane2.setStatus(CraneStatus.AVAILABLE);
        crane2.setActive(true);
        crane2.setLocation("Main Yard");

        Crane crane3 = new Crane();

        crane3.setCraneNumber("CR060T001");
        crane3.setType("60T");
        crane3.setCapacity(60);
        crane3.setManufacturer("Kobelco");
        crane3.setSerialNumber("SN-TEST-006");
        crane3.setYear(2024);
        crane3.setStatus(CraneStatus.IN_USE);
        crane3.setActive(true);
        crane3.setLocation("Project Site");

        craneRepository.save(crane1);
        craneRepository.save(crane2);
        craneRepository.save(crane3);

        long availableCount =
                craneRepository.countByStatus(CraneStatus.AVAILABLE);

        long inUseCount =
                craneRepository.countByStatus(CraneStatus.IN_USE);

        assertThat(availableCount).isEqualTo(2);
        assertThat(inUseCount).isEqualTo(1);
    }
}