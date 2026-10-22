package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.*;
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
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CraneRepository craneRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private JobCardRepository jobCardRepository;

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // existsByJobCard
    // ============================================================

    @Test
    void shouldReturnTrueWhenInvoiceExistsForJobCard() {

        TestData data = createTestData("INV-TEST-001", "JC-TEST-001");

        Invoice invoice = createInvoice(
                "INV-TEST-001",
                data.customer(),
                data.crane(),
                data.quotation(),
                data.jobCard(),
                LocalDate.of(2026, 9, 1),
                InvoiceStatus.DRAFT
        );

        invoiceRepository.save(invoice);

        boolean exists =
                invoiceRepository.existsByJobCard(data.jobCard());

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenInvoiceDoesNotExistForJobCard() {

        TestData data = createTestData("INV-TEST-002", "JC-TEST-002");

        boolean exists =
                invoiceRepository.existsByJobCard(data.jobCard());

        assertThat(exists).isFalse();
    }

    // ============================================================
    // findTopByOrderByIdDesc
    // ============================================================

    @Test
    void shouldFindInvoiceWithHighestId() {

        TestData data1 = createTestData("INV-TEST-003", "JC-TEST-003");
        TestData data2 = createTestData("INV-TEST-004", "JC-TEST-004");

        Invoice invoice1 = createInvoice(
                "INV-TEST-003",
                data1.customer(),
                data1.crane(),
                data1.quotation(),
                data1.jobCard(),
                LocalDate.of(2026, 9, 1),
                InvoiceStatus.DRAFT
        );

        Invoice invoice2 = createInvoice(
                "INV-TEST-004",
                data2.customer(),
                data2.crane(),
                data2.quotation(),
                data2.jobCard(),
                LocalDate.of(2026, 9, 2),
                InvoiceStatus.DRAFT
        );

        invoiceRepository.save(invoice1);
        invoiceRepository.save(invoice2);

        Optional<Invoice> result =
                invoiceRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getInvoiceNumber())
                .isEqualTo("INV-TEST-004");
    }

    // ============================================================
    // findByInvoiceDateBetween
    // ============================================================

    @Test
    void shouldFindInvoicesWithinInvoiceDateRange() {

        TestData data1 = createTestData("INV-TEST-005", "JC-TEST-005");
        TestData data2 = createTestData("INV-TEST-006", "JC-TEST-006");
        TestData data3 = createTestData("INV-TEST-007", "JC-TEST-007");

        invoiceRepository.save(createInvoice(
                "INV-TEST-005",
                data1.customer(),
                data1.crane(),
                data1.quotation(),
                data1.jobCard(),
                LocalDate.of(2026, 9, 5),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-006",
                data2.customer(),
                data2.crane(),
                data2.quotation(),
                data2.jobCard(),
                LocalDate.of(2026, 9, 15),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-007",
                data3.customer(),
                data3.crane(),
                data3.quotation(),
                data3.jobCard(),
                LocalDate.of(2026, 9, 25),
                InvoiceStatus.DRAFT
        ));

        List<Invoice> results =
                invoiceRepository.findByInvoiceDateBetween(
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 20)
                );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getInvoiceNumber())
                .isEqualTo("INV-TEST-006");
    }

    // ============================================================
    // findByCustomer_Id
    // ============================================================

    @Test
    void shouldFindInvoicesByCustomerId() {

        TestData customer1Data =
                createTestData("INV-TEST-008", "JC-TEST-008");

        TestData customer2Data =
                createTestData("INV-TEST-009", "JC-TEST-009");

        invoiceRepository.save(createInvoice(
                "INV-TEST-008",
                customer1Data.customer(),
                customer1Data.crane(),
                customer1Data.quotation(),
                customer1Data.jobCard(),
                LocalDate.of(2026, 9, 10),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-009",
                customer2Data.customer(),
                customer2Data.crane(),
                customer2Data.quotation(),
                customer2Data.jobCard(),
                LocalDate.of(2026, 9, 10),
                InvoiceStatus.DRAFT
        ));

        List<Invoice> results =
                invoiceRepository.findByCustomer_Id(
                        customer1Data.customer().getId()
                );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getInvoiceNumber())
                .isEqualTo("INV-TEST-008");
    }

    // ============================================================
    // findByStatus
    // ============================================================

    @Test
    void shouldFindInvoicesByStatus() {

        TestData draftData =
                createTestData("INV-TEST-010", "JC-TEST-010");

        TestData canceledData =
                createTestData("INV-TEST-011", "JC-TEST-011");

        invoiceRepository.save(createInvoice(
                "INV-TEST-010",
                draftData.customer(),
                draftData.crane(),
                draftData.quotation(),
                draftData.jobCard(),
                LocalDate.of(2026, 9, 10),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-011",
                canceledData.customer(),
                canceledData.crane(),
                canceledData.quotation(),
                canceledData.jobCard(),
                LocalDate.of(2026, 9, 11),
                InvoiceStatus.CANCELED
        ));

        List<Invoice> results =
                invoiceRepository.findByStatus(InvoiceStatus.DRAFT);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getInvoiceNumber())
                .isEqualTo("INV-TEST-010");
    }

    // ============================================================
    // findByStatusAndInvoiceDateBetween
    // ============================================================

    @Test
    void shouldFindInvoicesByStatusAndDateRange() {

        TestData data1 =
                createTestData("INV-TEST-012", "JC-TEST-012");

        TestData data2 =
                createTestData("INV-TEST-013", "JC-TEST-013");

        TestData data3 =
                createTestData("INV-TEST-014", "JC-TEST-014");

        invoiceRepository.save(createInvoice(
                "INV-TEST-012",
                data1.customer(),
                data1.crane(),
                data1.quotation(),
                data1.jobCard(),
                LocalDate.of(2026, 9, 5),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-013",
                data2.customer(),
                data2.crane(),
                data2.quotation(),
                data2.jobCard(),
                LocalDate.of(2026, 9, 15),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-014",
                data3.customer(),
                data3.crane(),
                data3.quotation(),
                data3.jobCard(),
                LocalDate.of(2026, 9, 15),
                InvoiceStatus.CANCELED
        ));

        List<Invoice> results =
                invoiceRepository.findByStatusAndInvoiceDateBetween(
                        InvoiceStatus.DRAFT,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 20)
                );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getInvoiceNumber())
                .isEqualTo("INV-TEST-013");
    }

    // ============================================================
    // countByStatus
    // ============================================================

    @Test
    void shouldCountInvoicesByStatus() {

        TestData data1 =
                createTestData("INV-TEST-015", "JC-TEST-015");

        TestData data2 =
                createTestData("INV-TEST-016", "JC-TEST-016");

        TestData data3 =
                createTestData("INV-TEST-017", "JC-TEST-017");

        invoiceRepository.save(createInvoice(
                "INV-TEST-015",
                data1.customer(),
                data1.crane(),
                data1.quotation(),
                data1.jobCard(),
                LocalDate.of(2026, 9, 10),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-016",
                data2.customer(),
                data2.crane(),
                data2.quotation(),
                data2.jobCard(),
                LocalDate.of(2026, 9, 11),
                InvoiceStatus.DRAFT
        ));

        invoiceRepository.save(createInvoice(
                "INV-TEST-017",
                data3.customer(),
                data3.crane(),
                data3.quotation(),
                data3.jobCard(),
                LocalDate.of(2026, 9, 12),
                InvoiceStatus.CANCELED
        ));

        long draftCount =
                invoiceRepository.countByStatus(InvoiceStatus.DRAFT);

        long canceledCount =
                invoiceRepository.countByStatus(InvoiceStatus.CANCELED);

        assertThat(draftCount).isEqualTo(2);
        assertThat(canceledCount).isEqualTo(1);
    }

    // ============================================================
    // Test Data Helpers
    // ============================================================

    private TestData createTestData(
            String invoiceNumber,
            String jobCardNumber
    ) {

        Customer customer = new Customer();
        customer.setCompanyName("Test Company " + invoiceNumber);
        customer.setRegistrationNo("REG-" + invoiceNumber);
        customer.setVatNo("VAT-" + invoiceNumber);
        customer.setCompanyAddress("Colombo");
        customer.setBillAddress("Colombo");
        customer.setContactPerson("Test Person");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail(
                invoiceNumber.toLowerCase() + "@example.com"
        );
        customer.setActive(true);

        customer = customerRepository.save(customer);

        Crane crane = new Crane();
        crane.setCraneNumber(
                "CR" + invoiceNumber.substring(invoiceNumber.length() - 3)
        );
        crane.setType("12T");
        crane.setCapacity(12);
        crane.setManufacturer("Liebherr");
        crane.setSerialNumber(
                "SN-" + invoiceNumber
        );
        crane.setYear(2022);
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(true);
        crane.setLocation("Main Yard");

        crane = craneRepository.save(crane);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(
                "QT-" + invoiceNumber.substring(invoiceNumber.length() - 3)
        );
        quotation.setCustomerId(customer);
        quotation.setType("12T");
        quotation.setValidFrom(LocalDate.of(2026, 9, 1));
        quotation.setValidTo(LocalDate.of(2026, 9, 30));
        quotation.setMinHours(BigDecimal.valueOf(6));
        quotation.setMinRate(BigDecimal.valueOf(18000));
        quotation.setMobilization(BigDecimal.valueOf(10000));
        quotation.setDescription("Test quotation");
        quotation.setActive(true);

        quotation = quotationRepository.save(quotation);

        User user = new User();
        user.setUsername(
                "testuser" + invoiceNumber.substring(invoiceNumber.length() - 3)
        );
        user.setEmail(
                "testuser" +
                        invoiceNumber.substring(invoiceNumber.length() - 3) +
                        "@example.com"
        );
        user.setPassword("password");
        user.setRole(UserRole.SALES);
        user.setActive(true);

        user = userRepository.save(user);

        JobCard jobCard = new JobCard();
        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(user);
        jobCard.setJobDate(LocalDate.of(2026, 9, 10));
        jobCard.setStartDate(LocalDate.of(2026, 9, 10));
        jobCard.setEndDate(LocalDate.of(2026, 9, 10));
        jobCard.setLocation("Test Site");
        jobCard.setDescription("Test job card");
        jobCard.setTotalWorkedHours(BigDecimal.valueOf(8));
        jobCard.setTotalBillableHours(BigDecimal.valueOf(8));
        jobCard.setStatus(JobCardStatus.COMPLETED);

        jobCard = jobCardRepository.save(jobCard);

        return new TestData(
                customer,
                crane,
                quotation,
                jobCard
        );
    }

    private Invoice createInvoice(
            String invoiceNumber,
            Customer customer,
            Crane crane,
            Quotation quotation,
            JobCard jobCard,
            LocalDate invoiceDate,
            InvoiceStatus status
    ) {

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomer(customer);
        invoice.setCrane(crane);
        invoice.setQuotation(quotation);
        invoice.setJobCard(jobCard);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setQty(BigDecimal.valueOf(8));
        invoice.setRate(BigDecimal.valueOf(18000));
        invoice.setAmount(BigDecimal.valueOf(144000));
        invoice.setMobilization(BigDecimal.valueOf(10000));
        invoice.setDescription("Test invoice");
        invoice.setStatus(status);
        invoice.setVersion(1);

        return invoice;
    }

    private record TestData(
            Customer customer,
            Crane crane,
            Quotation quotation,
            JobCard jobCard
    ) {
    }
}