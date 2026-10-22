package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "job_card")
@Data
public class JobCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jobCardNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customerId;

    @ManyToOne
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotationId;

    @ManyToOne
    @JoinColumn(name = "crane_id", nullable = false)
    private Crane craneId;

    @ManyToOne
    @JoinColumn(name = "sales_person_id", nullable = false)
    private User salesPersonId;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Operator operatorId;

    @Column(nullable = false)
    private LocalDate jobDate;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false)
    private String location;

    private String description;

    private BigDecimal totalWorkedHours;

    private BigDecimal totalBillableHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobCardStatus status;

}
