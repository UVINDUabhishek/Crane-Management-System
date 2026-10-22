package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.model.Crane;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.Invoice;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.Maintenance;
import com.uvindu.crane_management_system.model.Operator;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.CraneStatus;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.MaintenanceStatus;
import com.uvindu.crane_management_system.model.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportTestDataBuilder {

    // ---------------------------------------------------------
    // CUSTOMER
    // ---------------------------------------------------------

    public static Customer createCustomer(Long id, String companyName) {

        Customer customer = new Customer();

        customer.setId(id);
        customer.setCompanyName(companyName);

        return customer;
    }


    // ---------------------------------------------------------
    // CRANE
    // ---------------------------------------------------------

    public static Crane createCrane(
            Long id,
            String craneNumber,
            String type
    ) {

        Crane crane = new Crane();

        crane.setId(id);
        crane.setCraneNumber(craneNumber);
        crane.setType(type);
        crane.setStatus(CraneStatus.AVAILABLE);

        return crane;
    }


    // ---------------------------------------------------------
    // USER / SALES PERSON
    // ---------------------------------------------------------

    public static User createSalesPerson(
            Long id,
            String username
    ) {

        User user = new User();

        user.setId(id);
        user.setUsername(username);

        return user;
    }


    // ---------------------------------------------------------
    // OPERATOR
    // ---------------------------------------------------------

    public static Operator createOperator(
            Long id,
            String operatorName
    ) {

        Operator operator = new Operator();

        operator.setId(id);
        operator.setOperatorName(operatorName);

        return operator;
    }


    // ---------------------------------------------------------
    // JOB CARD
    // ---------------------------------------------------------

    public static JobCard createJobCard(
            Long id,
            String jobCardNumber,
            Customer customer,
            Crane crane,
            User salesPerson,
            Operator operator,
            JobCardStatus status
    ) {

        JobCard jobCard = new JobCard();

        jobCard.setId(id);
        jobCard.setJobCardNumber(jobCardNumber);

        jobCard.setCustomerId(customer);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);
        jobCard.setOperatorId(operator);

        jobCard.setJobDate(LocalDate.of(2026, 9, 20));
        jobCard.setStartDate(LocalDate.of(2026, 9, 20));
        jobCard.setEndDate(LocalDate.of(2026, 9, 21));

        jobCard.setLocation("Colombo");

        jobCard.setTotalWorkedHours(
                new BigDecimal("16")
        );

        jobCard.setTotalBillableHours(
                new BigDecimal("16")
        );

        jobCard.setStatus(status);

        return jobCard;
    }


    // ---------------------------------------------------------
    // INVOICE
    // ---------------------------------------------------------

    public static Invoice createInvoice(
            Long id,
            String invoiceNumber,
            Customer customer,
            Crane crane,
            JobCard jobCard,
            LocalDate invoiceDate,
            InvoiceStatus status,
            BigDecimal amount,
            BigDecimal mobilization
    ) {

        Invoice invoice = new Invoice();

        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);

        invoice.setCustomer(customer);
        invoice.setCrane(crane);
        invoice.setJobCard(jobCard);

        invoice.setInvoiceDate(invoiceDate);

        invoice.setQty(new BigDecimal("8"));
        invoice.setRate(new BigDecimal("25000"));

        invoice.setAmount(amount);
        invoice.setMobilization(mobilization);

        invoice.setDescription("Test invoice");

        invoice.setStatus(status);

        return invoice;
    }


    // ---------------------------------------------------------
    // TIMESHEET
    // ---------------------------------------------------------

    public static com.uvindu.crane_management_system.model.Timesheet createTimesheet(
            Long id,
            JobCard jobCard,
            BigDecimal workedHours
    ) {

        com.uvindu.crane_management_system.model.Timesheet timesheet =
                new com.uvindu.crane_management_system.model.Timesheet();

        timesheet.setId(id);
        timesheet.setJobCard(jobCard);
        timesheet.setDateOfWork(LocalDate.of(2026, 9, 20));
        timesheet.setWorkedHours(workedHours);
        timesheet.setDescription("Test timesheet");

        return timesheet;
    }


    // ---------------------------------------------------------
    // MAINTENANCE PERSON
    // ---------------------------------------------------------

    public static User createMaintenancePerson(
            Long id,
            String username
    ) {

        User user = new User();

        user.setId(id);
        user.setUsername(username);

        return user;
    }


    // ---------------------------------------------------------
    // MAINTENANCE
    // ---------------------------------------------------------

    public static Maintenance createMaintenance(
            Long id,
            String maintenanceNumber,
            Crane crane,
            User maintenancePerson,
            MaintenanceType maintenanceType,
            MaintenanceStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {

        Maintenance maintenance = new Maintenance();

        maintenance.setId(id);
        maintenance.setMaintenanceNumber(maintenanceNumber);

        maintenance.setCrane(crane);

        maintenance.setMaintenanceDate(
                LocalDate.of(2026, 9, 20)
        );

        maintenance.setMaintenanceStartDate(startDate);
        maintenance.setMaintenanceEndDate(endDate);

        maintenance.setMaintenanceType(maintenanceType);

        maintenance.setMaintenancePerson(maintenancePerson);

        maintenance.setStatus(status);

        return maintenance;
    }
}