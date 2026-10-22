package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.config.TestcontainersConfiguration;
import com.uvindu.crane_management_system.model.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    // ============================================================
    // existsByRegistrationNo
    // ============================================================

    @Test
    void shouldReturnTrueWhenRegistrationNumberExists() {

        Customer customer = new Customer();

        customer.setCompanyName("Test Construction (Pvt) Ltd");
        customer.setRegistrationNo("REG-TEST-001");
        customer.setVatNo("VAT-TEST-001");
        customer.setCompanyAddress("Colombo");
        customer.setBillAddress("Colombo");
        customer.setContactPerson("Test Person");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("test@example.com");
        customer.setActive(true);

        customerRepository.save(customer);

        boolean exists =
                customerRepository.existsByRegistrationNo("REG-TEST-001");

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRegistrationNumberDoesNotExist() {

        boolean exists =
                customerRepository.existsByRegistrationNo("REG-NOT-EXIST");

        assertThat(exists).isFalse();
    }
}