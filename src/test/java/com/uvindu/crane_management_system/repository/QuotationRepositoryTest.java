package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.Quotation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class QuotationRepositoryTest {

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldFindLatestQuotation() {
        Customer customer = createCustomer();

        Quotation firstQuotation = createQuotation(
                "QT-TEST-0001",
                customer
        );

        Quotation latestQuotation = createQuotation(
                "QT-TEST-0002",
                customer
        );

        quotationRepository.save(firstQuotation);
        quotationRepository.save(latestQuotation);

        Optional<Quotation> result =
                quotationRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getId())
                .isEqualTo(latestQuotation.getId());
        assertThat(result.get().getQuotationNumber())
                .isEqualTo("QT-TEST-0002");
    }

    @Test
    void shouldReturnEmptyWhenNoQuotationExists() {
        Optional<Quotation> result =
                quotationRepository.findTopByOrderByIdDesc();

        assertThat(result).isEmpty();
    }

    private Customer createCustomer() {
        Customer customer = new Customer();

        customer.setCompanyName("Quotation Test Customer");
        customer.setRegistrationNo("REG-QT-001");
        customer.setVatNo("VAT-QT-001");
        customer.setCompanyAddress("Test Address");
        customer.setBillAddress("Test Billing Address");
        customer.setContactPerson("Test Person");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("quotation-test@example.com");
        customer.setActive(true);

        return customerRepository.save(customer);
    }

    private Quotation createQuotation(
            String quotationNumber,
            Customer customer
    ) {
        Quotation quotation = new Quotation();

        quotation.setQuotationNumber(quotationNumber);
        quotation.setCustomerId(customer);
        quotation.setType("12T");
        quotation.setValidFrom(LocalDate.of(2026, 9, 1));
        quotation.setValidTo(LocalDate.of(2026, 12, 31));
        quotation.setMinHours(new BigDecimal("6.00"));
        quotation.setMinRate(new BigDecimal("25000.00"));
        quotation.setMobilization(new BigDecimal("15000.00"));
        quotation.setDescription("Test quotation");
        quotation.setActive(true);

        return quotation;
    }
}