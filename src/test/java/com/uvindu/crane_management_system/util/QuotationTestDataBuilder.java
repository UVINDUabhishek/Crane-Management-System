package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateQuotationRequest;
import com.uvindu.crane_management_system.dto.UpdateQuotationRequest;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.Quotation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class QuotationTestDataBuilder {

    public static Quotation createQuotation(Long id, String quotationNumber, Customer customer) {
        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setQuotationNumber(quotationNumber);
        quotation.setCustomerId(customer);
        quotation.setType("MOBILE");
        quotation.setValidFrom(LocalDate.now());
        quotation.setValidTo(LocalDate.now().plusMonths(6));
        quotation.setMinHours(new BigDecimal("8"));
        quotation.setMinRate(new BigDecimal("5000"));
        quotation.setMobilization(new BigDecimal("10000"));
        quotation.setDescription("Test quotation");
        quotation.setActive(true);
        return quotation;
    }

    public static CreateQuotationRequest createQuotationRequest(Long customerId) {
        CreateQuotationRequest request = new CreateQuotationRequest();
        request.setCustomerId(customerId);
        request.setType("MOBILE");
        request.setValidFrom(LocalDate.now());
        request.setValidTo(LocalDate.now().plusMonths(6));
        request.setMinHours(new BigDecimal("8"));
        request.setMinRate(new BigDecimal("5000"));
        request.setMobilization(new BigDecimal("10000"));
        request.setDescription("Test quotation");
        return request;
    }

    public static UpdateQuotationRequest createUpdateQuotationRequest() {
        UpdateQuotationRequest request = new UpdateQuotationRequest();
        request.setType("TOWER");
        request.setValidFrom(LocalDate.now());
        request.setValidTo(LocalDate.now().plusMonths(12));
        request.setMinHours(new BigDecimal("10"));
        request.setMinRate(new BigDecimal("6000"));
        request.setMobilization(new BigDecimal("15000"));
        request.setDescription("Updated quotation");
        return request;
    }
}
