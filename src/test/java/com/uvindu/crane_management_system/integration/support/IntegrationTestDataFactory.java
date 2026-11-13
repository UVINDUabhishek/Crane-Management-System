package com.uvindu.crane_management_system.integration.support;

import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import com.uvindu.crane_management_system.security.JwtService;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creates realistic, persisted integration-test fixtures and exercises the HTTP workflows. */
public final class IntegrationTestDataFactory {
    private final CustomerRepository customers;
    private final CraneRepository cranes;
    private final QuotationRepository quotations;
    private final JobCardRepository jobCards;
    private final TimesheetRepository timesheets;
    private final InvoiceRepository invoices;
    private final MockMvc mvc;
    private final JwtService jwt;

    public IntegrationTestDataFactory(CustomerRepository customers, CraneRepository cranes,
                                      QuotationRepository quotations, JobCardRepository jobCards,
                                      TimesheetRepository timesheets, InvoiceRepository invoices,
                                      MockMvc mvc, JwtService jwt) {
        this.customers = customers;
        this.cranes = cranes;
        this.quotations = quotations;
        this.jobCards = jobCards;
        this.timesheets = timesheets;
        this.invoices = invoices;
        this.mvc = mvc;
        this.jwt = jwt;
    }

    public Customer customer(String registration) {
        Customer customer = new Customer();
        customer.setCompanyName("Integration Test Customer");
        customer.setRegistrationNo(registration);
        customer.setVatNo("VAT001");
        customer.setCompanyAddress("Colombo");
        customer.setBillAddress("Colombo");
        customer.setContactPerson("Test Contact");
        customer.setContactNo("0771234567");
        customer.setCustomerEmail("customer@test.local");
        customer.setActive(true);
        return customers.save(customer);
    }

    public Crane fixtureCrane() {
        Crane crane = new Crane();
        crane.setCraneNumber("CR001T01");
        crane.setType("001");
        crane.setCapacity(25);
        crane.setManufacturer("Test Manufacturer");
        crane.setSerialNumber("BATCH2-CR-SERIAL");
        crane.setYear(2022);
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setActive(true);
        crane.setLocation("Colombo");
        return cranes.save(crane);
    }

    public Quotation fixtureQuotation(Customer customer) {
        Quotation quote = new Quotation();
        quote.setQuotationNumber("QT-0001");
        quote.setCustomerId(customer);
        quote.setType("001");
        quote.setValidFrom(LocalDate.of(2026, 10, 1));
        quote.setValidTo(LocalDate.of(2026, 12, 31));
        quote.setMinHours(new BigDecimal("8"));
        quote.setMinRate(new BigDecimal("1000"));
        quote.setMobilization(new BigDecimal("500"));
        quote.setDescription("Integration quote");
        quote.setActive(true);
        return quotations.save(quote);
    }

    public JobCard fixtureJobCard() throws Exception {
        Customer customer = customer("BATCH2-CUST");
        Crane crane = fixtureCrane();
        Quotation quotation = fixtureQuotation(customer);
        String body = """
                {"customerId":%d,"quotationId":%d,"craneId":%d,
                 "startDate":"2026-11-10","endDate":"2026-11-12",
                 "location":"Colombo","description":"Integration hire"}
                """.formatted(customer.getId(), quotation.getId(), crane.getId());
        mvc.perform(post("/api/job-card")
                        .header("Authorization", adminToken())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
        return jobCards.findAll().stream()
                .filter(job -> Objects.equals(job.getCraneId().getId(), crane.getId()))
                .findFirst().orElseThrow();
    }

    public Timesheet fixtureTimesheet(JobCard job) throws Exception {
        String body = """
                {"jobCardId":%d,"dateOfWork":"2020-01-01",
                 "workedHours":5,"description":"Day one"}
                """.formatted(job.getId());
        mvc.perform(post("/api/timesheets")
                        .header("Authorization", adminToken())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
        return timesheets.findAll().stream()
                .filter(sheet -> Objects.equals(sheet.getJobCard().getId(), job.getId()))
                .findFirst().orElseThrow();
    }

    public Invoice fixtureInvoice() throws Exception {
        JobCard job = fixtureJobCard();
        fixtureTimesheet(job);
        mvc.perform(patch("/api/job-card/{id}/complete", job.getId())
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
        return invoices.findAll().stream()
                .filter(invoice -> Objects.equals(invoice.getJobCard().getId(), job.getId()))
                .findFirst().orElseThrow();
    }

    public String maintenanceBody(Long craneId) {
        return """
                {"craneId":%d,"maintenanceStartDate":"2026-11-15",
                 "maintenanceEndDate":"2026-11-16","maintenanceType":"PREVENTIVE",
                 "description":"Scheduled inspection"}
                """.formatted(craneId);
    }

    private String adminToken() {
        return "Bearer " + jwt.generateToken("admin@test.local");
    }
}
