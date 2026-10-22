package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance")
@Data
public class Maintenance extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String maintenanceNumber;

    @ManyToOne
    @JoinColumn(name = "crane_id", nullable = false)
    private Crane crane;

    @Column(nullable = false)
    private LocalDate maintenanceDate;

    private LocalDate maintenanceStartDate;

    private LocalDate maintenanceEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType maintenanceType;

    private String description;

    @ManyToOne
    @JoinColumn(name = "maintenance_person_id")
    private User maintenancePerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;
}
