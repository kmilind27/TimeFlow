package com.company.leaveservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // From Gateway header — no FK to auth_db
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // CASUAL / SICK / EARNED / COMP_OFF
    @Column(name = "leave_type",
            nullable = false, length = 20)
    private String leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(nullable = false, length = 500)
    private String reason;

    // SUBMITTED / APPROVED / REJECTED / CANCELLED
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUBMITTED";

    // Manager who reviewed
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "manager_comment", length = 300)
    private String managerComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
	
}
