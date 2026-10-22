package com.uvindu.crane_management_system.repository;

import com.uvindu.crane_management_system.model.Invoice;
import com.uvindu.crane_management_system.model.InvoiceStatus;
import com.uvindu.crane_management_system.model.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByJobCard(JobCard jobCard);

    Optional<Invoice> findTopByOrderByIdDesc();

    List<Invoice> findByInvoiceDateBetween(
            LocalDate fromDate,
            LocalDate toDate
    );

    List<Invoice> findByCustomer_Id(Long CustomerId);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByStatusAndInvoiceDateBetween(
            InvoiceStatus status,
            LocalDate fromDate,
            LocalDate toDate
    );

    // Dashboard counts
    long countByStatus(InvoiceStatus status);

}
