package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "operators")
@Data
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String operatorName;

}
