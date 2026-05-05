package com.company.leaveservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances",
    uniqueConstraints = {
        // One balance record per user per leave type per year
        @UniqueConstraint(
            columnNames = {"user_id", "leave_type", "year"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leave_type",
            nullable = false, length = 20)
    private String leaveType;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private Double usedDays = 0.0;

    @Column(name = "remaining_days", nullable = false)
    private Double remainingDays;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Auto calculate remaining
        remainingDays = totalDays - usedDays;
    }
}