package com.uvindu.crane_management_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "timesheets",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"job_card_id", "date_of_work"})
        }
)
public class Timesheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timesheet_number", nullable = false, unique = true)
    private String timesheetNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Column(name = "date_of_work", nullable = false)
    private LocalDate dateOfWork;

    @Column(nullable = false)
    private BigDecimal workedHours;

    private String description;
}
