package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.exception.BusinessException;
import com.uvindu.crane_management_system.dto.CreateQuotationRequest;
import com.uvindu.crane_management_system.dto.QuotationResponse;
import com.uvindu.crane_management_system.dto.UpdateQuotationRequest;
import com.uvindu.crane_management_system.model.Customer;
import com.uvindu.crane_management_system.model.Quotation;
import com.uvindu.crane_management_system.repository.CustomerRepository;
import com.uvindu.crane_management_system.repository.QuotationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;

    public QuotationService(QuotationRepository quotationRepository, CustomerRepository customerRepository) {
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
    }

    // Create Quotation
    public QuotationResponse createQuotation(CreateQuotationRequest request) {

        Customer customerId = customerRepository.findById(request.getCustomerId())
                .orElseThrow(()-> BusinessException.notFound("Customer not found"));

        Quotation quotation = new Quotation();

        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setCustomerId(customerId);
        SchedulingService.validateDates(request.getValidFrom(), request.getValidTo());
        quotation.setType(request.getType());
        quotation.setValidFrom(request.getValidFrom());
        quotation.setValidTo(request.getValidTo());
        quotation.setMinHours(request.getMinHours());
        quotation.setMinRate(request.getMinRate());
        quotation.setMobilization(request.getMobilization());
        quotation.setDescription(request.getDescription());

        Quotation savedQuotation = quotationRepository.save(quotation);

        return mapToResponse(savedQuotation);

    }

    // Generate Quotation number
    private String generateQuotationNumber() {

        Optional<Quotation> latestQuotation =
                quotationRepository.findTopByOrderByIdDesc();

        if (latestQuotation.isEmpty()) {
            return "QT-0001";
        }

        String latestNumber = latestQuotation.get().getQuotationNumber();

        int number = Integer.parseInt(latestNumber.substring(3));

        return String.format("QT-%04d", number + 1);
    }


    // Get all quotations
    public List<QuotationResponse> getAllQuotations() {

        return quotationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get quotation by ID
    public QuotationResponse getQuotationById(long id) {

        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Quotation not found"));

        return mapToResponse(quotation);

    }

    // Update Quotation
    public QuotationResponse updateQuotation(long id, UpdateQuotationRequest request) {

        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Quotation not found"));

        SchedulingService.validateDates(request.getValidFrom(), request.getValidTo());
        quotation.setType(request.getType());
        quotation.setValidFrom(request.getValidFrom());
        quotation.setValidTo(request.getValidTo());
        quotation.setMinHours(request.getMinHours());
        quotation.setMinRate(request.getMinRate());
        quotation.setMobilization(request.getMobilization());
        quotation.setDescription(request.getDescription());

        Quotation updateQuotation = quotationRepository.save(quotation);

        return mapToResponse(updateQuotation);

    }

    // Delete Quotation
    public void deleteQuotation(long id) {

        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(()->
                        BusinessException.notFound("Quotation not found"));

        quotation.setActive(false);

        quotationRepository.save(quotation);

    }

    // Mapping method
    private QuotationResponse mapToResponse(Quotation quotation) {

        QuotationResponse response = new QuotationResponse();
        response.setId(quotation.getId());
        response.setCustomerId(quotation.getCustomerId().getId());

        response.setQuotationNumber(quotation.getQuotationNumber());
        response.setCustomerName(quotation.getCustomerId().getCompanyName());
        response.setType(quotation.getType());
        response.setValidFrom(quotation.getValidFrom());
        response.setValidTo(quotation.getValidTo());
        response.setMinHours(quotation.getMinHours());
        response.setMinRate(quotation.getMinRate());
        response.setMobilization(quotation.getMobilization());
        response.setDescription(quotation.getDescription());

        return response;

    }

}
