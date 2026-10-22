package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customers")
public class Customer extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String registrationNo;

    @Column(nullable = false)
    private String vatNo;

    @Column(nullable = false)
    private String companyAddress;

    private String billAddress;

    private String contactPerson;

    private String contactNo;

    private String customerEmail;

    private boolean active = true;
}
