package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.dto.CreateInvoiceRequest;
import com.uvindu.crane_management_system.dto.InvoiceResponse;
import com.uvindu.crane_management_system.dto.UpdateInvoiceRequest;
import com.uvindu.crane_management_system.model.Invoice;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.repository.InvoiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceResponse createInvoiceFromJobCard(JobCard jobCard) {

        // Prevent duplicate invoice
        if(invoiceRepository.existsByJobCard(jobCard)) {
            throw new RuntimeException("Invoice already exists for this Job Card");
        }

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setVersion(1);

        invoice.setCustomer(jobCard.getCustomerId());
        invoice.setCrane(jobCard.getCraneId());
        invoice.setQuotation(jobCard.getQuotationId());
        invoice.setJobCard(jobCard);

        invoice.setDescription(jobCard.getDescription());
        invoice.setInvoiceDate(LocalDate.now());

        // Use total billable hours calculated from Job-card timesheets
        BigDecimal qty = jobCard.getTotalBillableHours();

        if(qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Total billable hours are not available for this Job-card"
            );
        }

        BigDecimal rate = jobCard.getQuotationId().getMinRate();

        if(rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Minimum rate is not available in the quotation"
            );
        }

        BigDecimal mobilization = jobCard.getQuotationId().getMobilization();

        if(mobilization == null) {
            mobilization = BigDecimal.ZERO;
        }

        BigDecimal amount = qty.multiply(rate).add(mobilization);

        invoice.setQty(qty);
        invoice.setRate(rate);
        invoice.setMobilization(mobilization);
        invoice.setAmount(amount);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return mapToResponse(savedInvoice);

    }

    // Generate invoice number
    private String generateInvoiceNumber() {

        return invoiceRepository.findTopByOrderByIdDesc()
                .map(invoice -> {

                    String lastNumber = invoice.getInvoiceNumber();

                    int number = Integer.parseInt(lastNumber.substring(4));

                    return String.format("INV-%04d", number + 1);
                })
                .orElse("INV-0001");
    }

    // Complete invoice
    public InvoiceResponse completeInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Invoice not found"));

        if(invoice.getStatus() == InvoiceStatus.COMPLETED) {
            throw new RuntimeException("Invoice is already completed");
        }

        if(invoice.getStatus() == InvoiceStatus.REVISED) {
            throw new RuntimeException("Revised invoice cannot be completed");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELED) {
            throw new RuntimeException("Canceled invoice cannot be completed");
        }

        invoice.setStatus(InvoiceStatus.COMPLETED);

        Invoice completedInvoice = invoiceRepository.save(invoice);

        return mapToResponse(completedInvoice);
    }


    // Get all invoices
    public List<InvoiceResponse> getAllInvoices() {

        return invoiceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get invoice by ID
    public InvoiceResponse getInvoiceById(Long id) {

        Invoice invoice =  invoiceRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Invoice not found"));

        return mapToResponse(invoice);
    }

    // Update invoice
    public InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Invoice not found"));

        // Only DRAFT invoices can be updated
        if(invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException(
                    "Only draft invoices can be updated"
            );
        }

        BigDecimal qty = request.getQty();

        if(qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "qty must be greater than 0"
            );
        }

        BigDecimal rate = request.getRate();

        if(rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "rate must be greater than 0"
            );
        }

        invoice.setQty(qty);
        invoice.setRate(rate);
        invoice.setDescription(request.getDescription());

        BigDecimal mobilization = request.getMobilization();

        if(mobilization == null) {
            mobilization = BigDecimal.ZERO;
        }

        invoice.setMobilization(mobilization);

        // Calculate amount
        BigDecimal amount = request.getQty()
                .multiply(request.getRate())
                .add(mobilization);

        invoice.setAmount(amount);

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        return mapToResponse(updatedInvoice);
    }

    // Cancel invoice
    public InvoiceResponse cancelInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.CANCELED) {
            throw new RuntimeException("Invoice is already canceled");
        }

        if (invoice.getStatus() == InvoiceStatus.REVISED) {
            throw new RuntimeException("Revised invoice cannot be canceled");
        }

        invoice.setStatus(InvoiceStatus.CANCELED);

        Invoice canceledInvoice = invoiceRepository.save(invoice);

        return mapToResponse(canceledInvoice);
    }


    // Revise invoice
    @Transactional
    public InvoiceResponse reviseInvoice(Long id, CreateInvoiceRequest request) {

        Invoice oldInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (oldInvoice.getStatus() != InvoiceStatus.COMPLETED) {
            throw new RuntimeException(
                    "Only a completed invoice can be revised");
        }

        if (request.getQty() == null || request.getRate() == null) {
            throw new RuntimeException("Quantity and rate are required");
        }

        // Mark old invoice as revised
        oldInvoice.setStatus(InvoiceStatus.REVISED);
        invoiceRepository.save(oldInvoice);

        // Create new invoice
        Invoice newInvoice = new Invoice();

        newInvoice.setInvoiceNumber(generateInvoiceNumber());

        newInvoice.setCustomer(oldInvoice.getCustomer());
        newInvoice.setCrane(oldInvoice.getCrane());
        newInvoice.setQuotation(oldInvoice.getQuotation());
        newInvoice.setJobCard(oldInvoice.getJobCard());

        newInvoice.setInvoiceDate(LocalDate.now());

        newInvoice.setQty(request.getQty());
        newInvoice.setRate(request.getRate());

        BigDecimal mobilization = request.getMobilization();

        if(mobilization == null) {
            mobilization = BigDecimal.ZERO;
        }

        newInvoice.setMobilization(mobilization);

        newInvoice.setAmount(
                request.getQty()
                        .multiply(request.getRate())
                        .add(mobilization)
        );

        newInvoice.setDescription(request.getDescription());

        newInvoice.setStatus(InvoiceStatus.DRAFT);

        newInvoice.setVersion(oldInvoice.getVersion() + 1);

        newInvoice.setOriginalInvoice(oldInvoice);

        Invoice revisedInvoice = invoiceRepository.save(newInvoice);

        return mapToResponse(revisedInvoice);
    }

    // Mapping method
    private InvoiceResponse mapToResponse(Invoice invoice) {

        InvoiceResponse response = new InvoiceResponse();

        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setCustomer(invoice.getCustomer().getCompanyName());
        response.setCrane(invoice.getCrane().getSerialNumber());
        response.setQuotation(invoice.getQuotation().getQuotationNumber());
        response.setJobCard(invoice.getJobCard().getJobCardNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setQty(invoice.getQty());
        response.setRate(invoice.getRate());
        response.setMobilization(invoice.getMobilization());
        response.setAmount(invoice.getAmount());
        response.setDescription(invoice.getDescription());
        response.setStatus(invoice.getStatus());
        response.setVersion(invoice.getVersion());

        return response;
    }

}
