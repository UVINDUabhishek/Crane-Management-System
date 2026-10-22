package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateInvoiceRequest;
import com.uvindu.crane_management_system.dto.UpdateInvoiceRequest;
import com.uvindu.crane_management_system.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceTestDataBuilder {

    public static Invoice createInvoice(Long id, String invoiceNumber, InvoiceStatus status) {

        Invoice invoice = new Invoice();

        invoice.setId(id);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setQty(new BigDecimal("40"));
        invoice.setRate(new BigDecimal("5000"));
        invoice.setMobilization(new BigDecimal("10000"));
        invoice.setAmount(new BigDecimal("210000"));
        invoice.setDescription("Test invoice");
        invoice.setStatus(status);
        invoice.setVersion(1);

        Customer customer = CustomerTestDataBuilder.createCustomer(1L);

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setSerialNumber("SL-001");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-001");
        quotation.setMinRate(new BigDecimal("5000"));
        quotation.setMobilization(new BigDecimal("10000"));

        JobCard jobCard = new JobCard();
        jobCard.setId(1L);
        jobCard.setJobCardNumber("JC-0001");
        jobCard.setDescription("Test job card");
        jobCard.setTotalBillableHours(new BigDecimal("40"));
        jobCard.setStatus(JobCardStatus.COMPLETED);

        invoice.setCustomer(customer);
        invoice.setCrane(crane);
        invoice.setQuotation(quotation);
        invoice.setJobCard(jobCard);

        return invoice;
    }

    public static CreateInvoiceRequest createInvoiceRequest() {

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setQty(new BigDecimal("45"));
        request.setRate(new BigDecimal("5500"));
        request.setMobilization(new BigDecimal("12000"));
        request.setDescription("Revised invoice");

        return request;
    }

    public static UpdateInvoiceRequest createUpdateInvoiceRequest() {

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setQty(new BigDecimal("42"));
        request.setRate(new BigDecimal("5200"));
        request.setMobilization(new BigDecimal("11000"));
        request.setDescription("Updated invoice");

        return request;
    }

    public static JobCard createCompletedJobCardWithInvoiceData() {

        JobCard jobCard = new JobCard();
        jobCard.setId(1L);
        jobCard.setJobCardNumber("JC-0001");
        jobCard.setDescription("Test job card");
        jobCard.setTotalBillableHours(new BigDecimal("40"));
        jobCard.setStatus(JobCardStatus.COMPLETED);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setSerialNumber("SN-001");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");
        quotation.setMinRate(new BigDecimal("5000"));
        quotation.setMobilization(new BigDecimal("10000"));

        jobCard.setCustomerId(customer);
        jobCard.setCraneId(crane);
        jobCard.setQuotationId(quotation);

        return jobCard;
    }
}
