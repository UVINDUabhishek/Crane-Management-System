package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Quotation;
import com.uvindu.crane_management_system.model.Timesheet;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TimesheetRepositoryTest {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private JobCardRepository jobCardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CraneRepository craneRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

    private Customer customer;
    private Crane crane;
    private Quotation quotation;
    private User salesPerson;
    private JobCard jobCard;

    @BeforeEach
    void setUp() {
        customer = createCustomer();
        crane = createCrane();
        quotation = createQuotation(customer);
        salesPerson = createSalesPerson();

        jobCard = createJobCard(
                "JC-TS-TEST-001",
                customer,
                quotation,
                crane,
                salesPerson
        );
    }

    @Test
    void shouldFindLatestTimesheet() {
        Timesheet firstTimesheet = createTimesheet(
                "TS-TEST-0001",
                jobCard,
                LocalDate.of(2026, 9, 1)
        );

        Timesheet latestTimesheet = createTimesheet(
                "TS-TEST-0002",
                jobCard,
                LocalDate.of(2026, 9, 2)
        );

        timesheetRepository.save(firstTimesheet);
        timesheetRepository.save(latestTimesheet);

        Optional<Timesheet> result =
                timesheetRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getId())
                .isEqualTo(latestTimesheet.getId());
        assertThat(result.get().getTimesheetNumber())
                .isEqualTo("TS-TEST-0002");
    }

    @Test
    void shouldReturnEmptyWhenNoTimesheetExists() {
        Optional<Timesheet> result =
                timesheetRepository.findTopByOrderByIdDesc();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenTimesheetExistsForJobCardAndDate() {
        Timesheet timesheet = createTimesheet(
                "TS-TEST-0011",
                jobCard,
                LocalDate.of(2026, 9, 5)
        );

        timesheetRepository.save(timesheet);

        boolean result =
                timesheetRepository.existsByJobCardAndDateOfWork(
                        jobCard,
                        LocalDate.of(2026, 9, 5)
                );

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTimesheetDoesNotExistForJobCardAndDate() {
        Timesheet timesheet = createTimesheet(
                "TS-TEST-0021",
                jobCard,
                LocalDate.of(2026, 9, 5)
        );

        timesheetRepository.save(timesheet);

        boolean result =
                timesheetRepository.existsByJobCardAndDateOfWork(
                        jobCard,
                        LocalDate.of(2026, 9, 6)
                );

        assertThat(result).isFalse();
    }

    @Test
    void shouldFindTimesheetByJobCardAndDate() {
        Timesheet timesheet = createTimesheet(
                "TS-TEST-0031",
                jobCard,
                LocalDate.of(2026, 9, 7)
        );

        timesheetRepository.save(timesheet);

        Optional<Timesheet> result =
                timesheetRepository.findByJobCardAndDateOfWork(
                        jobCard,
                        LocalDate.of(2026, 9, 7)
                );

        assertThat(result).isPresent();
        assertThat(result.get().getTimesheetNumber())
                .isEqualTo("TS-TEST-0031");
        assertThat(result.get().getDateOfWork())
                .isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    void shouldReturnEmptyWhenJobCardAndDateDoNotMatch() {
        Timesheet timesheet = createTimesheet(
                "TS-TEST-0041",
                jobCard,
                LocalDate.of(2026, 9, 7)
        );

        timesheetRepository.save(timesheet);

        Optional<Timesheet> result =
                timesheetRepository.findByJobCardAndDateOfWork(
                        jobCard,
                        LocalDate.of(2026, 9, 8)
                );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindAllTimesheetsForJobCard() {
        JobCard secondJobCard = createJobCard(
                "JC-TS-TEST-002",
                customer,
                quotation,
                crane,
                salesPerson
        );

        timesheetRepository.save(
                createTimesheet(
                        "TS-TEST-0051",
                        jobCard,
                        LocalDate.of(2026, 9, 10)
                )
        );

        timesheetRepository.save(
                createTimesheet(
                        "TS-TEST-0052",
                        jobCard,
                        LocalDate.of(2026, 9, 11)
                )
        );

        timesheetRepository.save(
                createTimesheet(
                        "TS-TEST-0053",
                        secondJobCard,
                        LocalDate.of(2026, 9, 10)
                )
        );

        List<Timesheet> result =
                timesheetRepository.findByJobCard(jobCard);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Timesheet::getTimesheetNumber)
                .containsExactlyInAnyOrder(
                        "TS-TEST-0051",
                        "TS-TEST-0052"
                );
    }

    @Test
    void shouldFindAllTimesheetsWithJobCardAndCrane() {
        Timesheet firstTimesheet = timesheetRepository.save(
                createTimesheet(
                        "TS-TEST-0061",
                        jobCard,
                        LocalDate.of(2026, 9, 12)
                )
        );

        List<Timesheet> result =
                timesheetRepository.findAllWithJobCardAndCrane();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId())
                .isEqualTo(firstTimesheet.getId());

        assertThat(result.getFirst().getJobCard())
                .isNotNull();

        assertThat(result.getFirst().getJobCard().getCraneId())
                .isNotNull();

        assertThat(result.getFirst().getJobCard().getCraneId().getCraneNumber())
                .isEqualTo("CR012TS901");
    }

    @Test
    void shouldFindCompletedJobTimesheetsWithinDateRange() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        jobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(jobCard);

        timesheetRepository.saveAllAndFlush(List.of(
                createTimesheet(
                        "TS-IDLE-001",
                        jobCard,
                        LocalDate.of(2026, 10, 12)
                ),
                createTimesheet(
                        "TS-IDLE-002",
                        jobCard,
                        LocalDate.of(2026, 10, 15)
                ),
                createTimesheet(
                        "TS-IDLE-003",
                        jobCard,
                        LocalDate.of(2026, 10, 18)
                )
        ));

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(3);

        assertThat(result)
                .extracting(Timesheet::getTimesheetNumber)
                .containsExactlyInAnyOrder(
                        "TS-IDLE-001",
                        "TS-IDLE-002",
                        "TS-IDLE-003"
                );

        assertThat(result)
                .extracting(Timesheet::getDateOfWork)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 15),
                        LocalDate.of(2026, 10, 18)
                );
    }

    @Test
    void shouldExcludeTimesheetsFromPendingAndCanceledJobs() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        // Existing job card is PENDING
        JobCard completedJobCard = createJobCard(
                "JC-TS-IDLE-002",
                customer,
                quotation,
                crane,
                salesPerson
        );

        completedJobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(completedJobCard);

        JobCard canceledJobCard = createJobCard(
                "JC-TS-IDLE-003",
                customer,
                quotation,
                crane,
                salesPerson
        );

        canceledJobCard.setStatus(JobCardStatus.CANCELED);
        jobCardRepository.saveAndFlush(canceledJobCard);

        timesheetRepository.saveAllAndFlush(List.of(
                createTimesheet(
                        "TS-IDLE-004",
                        jobCard,
                        LocalDate.of(2026, 10, 12)
                ),
                createTimesheet(
                        "TS-IDLE-005",
                        completedJobCard,
                        LocalDate.of(2026, 10, 13)
                ),
                createTimesheet(
                        "TS-IDLE-006",
                        canceledJobCard,
                        LocalDate.of(2026, 10, 14)
                )
        ));

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        assertThat(result.getFirst().getTimesheetNumber())
                .isEqualTo("TS-IDLE-005");

        assertThat(result.getFirst().getJobCard().getStatus())
                .isEqualTo(JobCardStatus.COMPLETED);
    }

    @Test
    void shouldExcludeTimesheetsOutsideReportingPeriod() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        jobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(jobCard);

        timesheetRepository.saveAllAndFlush(List.of(
                createTimesheet(
                        "TS-IDLE-007",
                        jobCard,
                        LocalDate.of(2026, 10, 9)
                ),
                createTimesheet(
                        "TS-IDLE-008",
                        jobCard,
                        LocalDate.of(2026, 10, 21)
                )
        ));

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldIncludeTimesheetsOnReportingPeriodBoundaries() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        jobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(jobCard);

        timesheetRepository.saveAllAndFlush(List.of(
                createTimesheet(
                        "TS-IDLE-009",
                        jobCard,
                        fromDate
                ),
                createTimesheet(
                        "TS-IDLE-010",
                        jobCard,
                        toDate
                )
        ));

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Timesheet::getDateOfWork)
                .containsExactlyInAnyOrder(fromDate, toDate);
    }

    @Test
    void shouldFindCompletedJobTimesheetsForMultipleCranes() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        // Create a second crane
        Crane secondCrane = new Crane();

        secondCrane.setCraneNumber("CR060TS902");
        secondCrane.setType("60T");
        secondCrane.setCapacity(60.0);
        secondCrane.setManufacturer("Test Manufacturer");
        secondCrane.setSerialNumber("SN-TS-002");
        secondCrane.setYear(2025);
        secondCrane.setStatus(CraneStatus.AVAILABLE);
        secondCrane.setActive(true);
        secondCrane.setLocation("Test Yard");

        secondCrane = craneRepository.saveAndFlush(secondCrane);

        jobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(jobCard);

        JobCard secondJobCard = createJobCard(
                "JC-TS-IDLE-004",
                customer,
                quotation,
                secondCrane,
                salesPerson
        );

        secondJobCard.setStatus(JobCardStatus.COMPLETED);
        jobCardRepository.saveAndFlush(secondJobCard);

        timesheetRepository.saveAllAndFlush(List.of(
                createTimesheet(
                        "TS-IDLE-011",
                        jobCard,
                        LocalDate.of(2026, 10, 12)
                ),
                createTimesheet(
                        "TS-IDLE-012",
                        secondJobCard,
                        LocalDate.of(2026, 10, 13)
                )
        ));

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(
                        t -> t.getJobCard()
                                .getCraneId()
                                .getCraneNumber()
                )
                .containsExactlyInAnyOrder(
                        "CR012TS901",
                        "CR060TS902"
                );
    }

    @Test
    void shouldReturnEmptyWhenNoCompletedJobTimesheetsMatch() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        // Existing job card remains PENDING
        timesheetRepository.saveAndFlush(
                createTimesheet(
                        "TS-IDLE-013",
                        jobCard,
                        LocalDate.of(2026, 10, 15)
                )
        );

        // Act
        List<Timesheet> result =
                timesheetRepository.findWorkWithinPeriod(
                        JobCardStatus.COMPLETED,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).isEmpty();
    }



    private Customer createCustomer() {
        Customer customer = new Customer();

        customer.setCompanyName("Timesheet Test Customer");
        customer.setRegistrationNo("REG-TS-001");
        customer.setVatNo("VAT-TS-001");
        customer.setCompanyAddress("Test Address");
        customer.setBillAddress("Test Billing Address");
        customer.setContactPerson("Test Person");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("timesheet-test@example.com");
        customer.setActive(true);

        return customerRepository.save(customer);
    }

    private Crane createCrane() {
        Crane crane = new Crane();

        crane.setCraneNumber("CR012TS901");
        crane.setType("12T");
        crane.setCapacity(12.0);
        crane.setManufacturer("Test Manufacturer");
        crane.setSerialNumber("SN-TS-001");
        crane.setYear(2025);
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(true);
        crane.setLocation("Test Yard");

        return craneRepository.save(crane);
    }

    private Quotation createQuotation(Customer customer) {
        Quotation quotation = new Quotation();

        quotation.setQuotationNumber("QT-TS-TEST-001");
        quotation.setCustomerId(customer);
        quotation.setType("12T");
        quotation.setValidFrom(LocalDate.of(2026, 9, 1));
        quotation.setValidTo(LocalDate.of(2026, 12, 31));
        quotation.setMinHours(new BigDecimal("6.00"));
        quotation.setMinRate(new BigDecimal("25000.00"));
        quotation.setMobilization(new BigDecimal("15000.00"));
        quotation.setDescription("Test quotation");
        quotation.setActive(true);

        return quotationRepository.save(quotation);
    }

    private User createSalesPerson() {
        User user = new User();

        user.setUsername("sales-ts-test");
        user.setEmail("sales-ts-test@example.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.SALES);
        user.setActive(true);

        return userRepository.save(user);
    }

    private JobCard createJobCard(
            String jobCardNumber,
            Customer customer,
            Quotation quotation,
            Crane crane,
            User salesPerson
    ) {
        JobCard jobCard = new JobCard();

        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);
        jobCard.setJobDate(LocalDate.of(2026, 9, 1));
        jobCard.setStartDate(LocalDate.of(2026, 9, 1));
        jobCard.setEndDate(LocalDate.of(2026, 9, 1));
        jobCard.setLocation("Test Location");
        jobCard.setDescription("Test job card");
        jobCard.setTotalWorkedHours(new BigDecimal("8.00"));
        jobCard.setTotalBillableHours(new BigDecimal("8.00"));
        jobCard.setStatus(JobCardStatus.PENDING);

        return jobCardRepository.save(jobCard);
    }

    private Timesheet createTimesheet(
            String timesheetNumber,
            JobCard jobCard,
            LocalDate dateOfWork
    ) {
        Timesheet timesheet = new Timesheet();

        timesheet.setTimesheetNumber(timesheetNumber);
        timesheet.setJobCard(jobCard);
        timesheet.setDateOfWork(dateOfWork);
        timesheet.setWorkedHours(new BigDecimal("8.00"));
        timesheet.setDescription("Test timesheet");

        return timesheet;
    }
}