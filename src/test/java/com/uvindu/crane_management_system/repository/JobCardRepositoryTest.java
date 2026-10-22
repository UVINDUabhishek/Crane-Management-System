package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Quotation;
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
class JobCardRepositoryTest {

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
    private Customer secondCustomer;
    private Crane crane;
    private Crane secondCrane;
    private Quotation quotation;
    private User salesPerson;

    @BeforeEach
    void setUp() {
        customer = createCustomer("ABC Construction", "REG-JC-001");
        secondCustomer = createCustomer("XYZ Engineering", "REG-JC-002");

        crane = createCrane("CR012T901", "12T", "SN-JC-001");
        secondCrane = createCrane("CR060T901", "60T", "SN-JC-002");

        quotation = createQuotation();

        salesPerson = createSalesPerson();
    }

    @Test
    void shouldFindLatestJobCard() {
        JobCard firstJobCard = createJobCard(
                "JC-TEST-0001",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 1),
                JobCardStatus.PENDING
        );

        JobCard latestJobCard = createJobCard(
                "JC-TEST-0002",
                secondCustomer,
                quotation,
                secondCrane,
                salesPerson,
                LocalDate.of(2026, 9, 2),
                JobCardStatus.COMPLETED
        );

        jobCardRepository.save(firstJobCard);
        jobCardRepository.save(latestJobCard);

        Optional<JobCard> result = jobCardRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latestJobCard.getId());
    }

    @Test
    void shouldReturnEmptyWhenNoJobCardsExist() {
        Optional<JobCard> result = jobCardRepository.findTopByOrderByIdDesc();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindJobCardsByJobDateBetween() {
        JobCard jobCard1 = createJobCard(
                "JC-TEST-0011",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 5),
                JobCardStatus.PENDING
        );

        JobCard jobCard2 = createJobCard(
                "JC-TEST-0012",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 10),
                JobCardStatus.COMPLETED
        );

        JobCard jobCard3 = createJobCard(
                "JC-TEST-0013",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 20),
                JobCardStatus.CANCELED
        );

        jobCardRepository.saveAll(List.of(jobCard1, jobCard2, jobCard3));

        List<JobCard> result = jobCardRepository.findByJobDateBetween(
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 10)
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(JobCard::getJobCardNumber)
                .containsExactlyInAnyOrder("JC-TEST-0011", "JC-TEST-0012");
    }

    @Test
    void shouldFindJobCardsByCustomerId() {
        JobCard customerJobCard = createJobCard(
                "JC-TEST-0021",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 11),
                JobCardStatus.PENDING
        );

        JobCard otherCustomerJobCard = createJobCard(
                "JC-TEST-0022",
                secondCustomer,
                quotation,
                secondCrane,
                salesPerson,
                LocalDate.of(2026, 9, 12),
                JobCardStatus.PENDING
        );

        jobCardRepository.saveAll(List.of(customerJobCard, otherCustomerJobCard));

        List<JobCard> result =
                jobCardRepository.findByCustomerId_Id(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getJobCardNumber())
                .isEqualTo("JC-TEST-0021");
    }

    @Test
    void shouldFindJobCardsByCraneId() {
        JobCard craneJobCard = createJobCard(
                "JC-TEST-0031",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 13),
                JobCardStatus.PENDING
        );

        JobCard otherCraneJobCard = createJobCard(
                "JC-TEST-0032",
                customer,
                quotation,
                secondCrane,
                salesPerson,
                LocalDate.of(2026, 9, 14),
                JobCardStatus.COMPLETED
        );

        jobCardRepository.saveAll(List.of(craneJobCard, otherCraneJobCard));

        List<JobCard> result =
                jobCardRepository.findByCraneId_Id(crane.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getJobCardNumber())
                .isEqualTo("JC-TEST-0031");
    }

    @Test
    void shouldFindJobCardsByStatus() {
        JobCard pendingJobCard = createJobCard(
                "JC-TEST-0041",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 15),
                JobCardStatus.PENDING
        );

        JobCard completedJobCard = createJobCard(
                "JC-TEST-0042",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 16),
                JobCardStatus.COMPLETED
        );

        jobCardRepository.saveAll(List.of(pendingJobCard, completedJobCard));

        List<JobCard> result =
                jobCardRepository.findByStatus(JobCardStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getJobCardNumber())
                .isEqualTo("JC-TEST-0041");
    }

    @Test
    void shouldFindJobCardsBySalesPersonId() {
        User secondSalesPerson = createUser(
                "sales-test-002",
                "sales-test-002@example.com"
        );

        JobCard firstSalesJobCard = createJobCard(
                "JC-TEST-0051",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 17),
                JobCardStatus.PENDING
        );

        JobCard secondSalesJobCard = createJobCard(
                "JC-TEST-0052",
                customer,
                quotation,
                crane,
                secondSalesPerson,
                LocalDate.of(2026, 9, 18),
                JobCardStatus.PENDING
        );

        jobCardRepository.saveAll(List.of(firstSalesJobCard, secondSalesJobCard));

        List<JobCard> result =
                jobCardRepository.findBySalesPersonId_Id(salesPerson.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getJobCardNumber())
                .isEqualTo("JC-TEST-0051");
    }

    @Test
    void shouldCountJobCardsByStatus() {
        JobCard pendingJobCard1 = createJobCard(
                "JC-TEST-0061",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 19),
                JobCardStatus.PENDING
        );

        JobCard pendingJobCard2 = createJobCard(
                "JC-TEST-0062",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 20),
                JobCardStatus.PENDING
        );

        JobCard completedJobCard = createJobCard(
                "JC-TEST-0063",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 9, 21),
                JobCardStatus.COMPLETED
        );

        jobCardRepository.saveAll(
                List.of(pendingJobCard1, pendingJobCard2, completedJobCard)
        );

        long pendingCount =
                jobCardRepository.countByStatus(JobCardStatus.PENDING);

        long completedCount =
                jobCardRepository.countByStatus(JobCardStatus.COMPLETED);

        assertThat(pendingCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(1);
    }


    @Test
    void shouldFindPendingReservationsOverlappingDateRange() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        JobCard startsBefore = createJobCard(
                "JC-IDLE-001",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 5),
                JobCardStatus.PENDING
        );
        startsBefore.setEndDate(LocalDate.of(2026, 10, 12));

        JobCard withinRange = createJobCard(
                "JC-IDLE-002",
                customer,
                quotation,
                secondCrane,
                salesPerson,
                LocalDate.of(2026, 10, 14),
                JobCardStatus.PENDING
        );
        withinRange.setEndDate(LocalDate.of(2026, 10, 16));

        JobCard endsAfter = createJobCard(
                "JC-IDLE-003",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 18),
                JobCardStatus.PENDING
        );
        endsAfter.setEndDate(LocalDate.of(2026, 10, 25));

        jobCardRepository.saveAllAndFlush(
                List.of(startsBefore, withinRange, endsAfter)
        );

        // Act
        List<JobCard> result =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(3);

        assertThat(result)
                .extracting(JobCard::getJobCardNumber)
                .containsExactlyInAnyOrder(
                        "JC-IDLE-001",
                        "JC-IDLE-002",
                        "JC-IDLE-003"
                );
    }


    @Test
    void shouldExcludePendingReservationsOutsideDateRange() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        JobCard beforeRange = createJobCard(
                "JC-IDLE-004",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 1),
                JobCardStatus.PENDING
        );
        beforeRange.setEndDate(LocalDate.of(2026, 10, 9));

        JobCard afterRange = createJobCard(
                "JC-IDLE-005",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 21),
                JobCardStatus.PENDING
        );
        afterRange.setEndDate(LocalDate.of(2026, 10, 25));

        jobCardRepository.saveAllAndFlush(
                List.of(beforeRange, afterRange)
        );

        // Act
        List<JobCard> result =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).isEmpty();
    }


    @Test
    void shouldOnlyFindPendingJobCardsAsReservations() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        JobCard pending = createJobCard(
                "JC-IDLE-006",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 12),
                JobCardStatus.PENDING
        );

        JobCard completed = createJobCard(
                "JC-IDLE-007",
                customer,
                quotation,
                secondCrane,
                salesPerson,
                LocalDate.of(2026, 10, 13),
                JobCardStatus.COMPLETED
        );

        JobCard canceled = createJobCard(
                "JC-IDLE-008",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 14),
                JobCardStatus.CANCELED
        );

        jobCardRepository.saveAllAndFlush(
                List.of(pending, completed, canceled)
        );

        // Act
        List<JobCard> result =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        assertThat(result.getFirst().getJobCardNumber())
                .isEqualTo("JC-IDLE-006");

        assertThat(result.getFirst().getStatus())
                .isEqualTo(JobCardStatus.PENDING);
    }

    @Test
    void shouldIncludeReservationsOnDateRangeBoundaries() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        JobCard endsOnFirstDay = createJobCard(
                "JC-IDLE-009",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 8),
                JobCardStatus.PENDING
        );
        endsOnFirstDay.setEndDate(fromDate);

        JobCard startsOnLastDay = createJobCard(
                "JC-IDLE-010",
                customer,
                quotation,
                secondCrane,
                salesPerson,
                toDate,
                JobCardStatus.PENDING
        );

        jobCardRepository.saveAllAndFlush(
                List.of(endsOnFirstDay, startsOnLastDay)
        );

        // Act
        List<JobCard> result =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result)
                .extracting(JobCard::getJobCardNumber)
                .containsExactlyInAnyOrder(
                        "JC-IDLE-009",
                        "JC-IDLE-010"
                );
    }

    @Test
    void shouldFindOpenEndedReservationsWithAssociatedCrane() {

        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 10, 10);
        LocalDate toDate = LocalDate.of(2026, 10, 20);

        JobCard openEnded = createJobCard(
                "JC-IDLE-011",
                customer,
                quotation,
                crane,
                salesPerson,
                LocalDate.of(2026, 10, 15),
                JobCardStatus.PENDING
        );

        openEnded.setEndDate(null);

        jobCardRepository.saveAndFlush(openEnded);

        // Act
        List<JobCard> result =
                jobCardRepository.findReservationsOverlapping(
                        JobCardStatus.PENDING,
                        fromDate,
                        toDate
                );

        // Assert
        assertThat(result).hasSize(1);

        JobCard reservation = result.getFirst();

        assertThat(reservation.getJobCardNumber())
                .isEqualTo("JC-IDLE-011");

        assertThat(reservation.getEndDate()).isNull();

        assertThat(reservation.getCraneId()).isNotNull();

        assertThat(reservation.getCraneId().getId())
                .isEqualTo(crane.getId());

        assertThat(reservation.getCraneId().getCraneNumber())
                .isEqualTo("CR012T901");
    }


    private Customer createCustomer(String companyName, String registrationNo) {
        Customer customer = new Customer();

        customer.setCompanyName(companyName);
        customer.setRegistrationNo(registrationNo);
        customer.setVatNo("VAT-" + registrationNo);
        customer.setCompanyAddress("Test Address");
        customer.setBillAddress("Test Billing Address");
        customer.setContactPerson("Test Person");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("test-" + registrationNo + "@example.com");
        customer.setActive(true);

        return customerRepository.save(customer);
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

    private Quotation createQuotation() {
        Quotation quotation = new Quotation();

        quotation.setQuotationNumber("QT-JC-TEST-001");
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
        return createUser(
                "sales-test-001",
                "sales-test-001@example.com"
        );
    }

    private User createUser(String username, String email) {
        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
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
            User salesPerson,
            LocalDate jobDate,
            JobCardStatus status
    ) {
        JobCard jobCard = new JobCard();

        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);
        jobCard.setJobDate(jobDate);
        jobCard.setStartDate(jobDate);
        jobCard.setEndDate(jobDate);
        jobCard.setLocation("Test Location");
        jobCard.setDescription("Test job card");
        jobCard.setTotalWorkedHours(new BigDecimal("8.00"));
        jobCard.setTotalBillableHours(new BigDecimal("8.00"));
        jobCard.setStatus(status);

        return jobCard;
    }
}