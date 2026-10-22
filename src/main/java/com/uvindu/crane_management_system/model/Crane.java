package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cranes")
public class Crane extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String craneNumber;

    @Column(nullable = false)
    private String type;

    private double capacity;

    private String manufacturer;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CraneStatus status;

    private boolean active = true;

    private String location;
}
