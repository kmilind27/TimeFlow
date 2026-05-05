package com.company.timesheetservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "timesheets", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"user_id", "week_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timesheet {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "user_id", nullable = false)
	private Long userId;
	
	@Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;
	
	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "DRAFT"; // DRAFT / SUBMITTED / APPROVED / REJECTED
	
	@Column(name = "total_hours")
	@Builder.Default
	private Double totalHours = 0.0;
	
	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;
	
	 // Manager who reviewed
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
	
    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimesheetEntry> entries;
    
    @PrePersist
    public void onCreate() {
    	createdAt = LocalDateTime.now();
    	updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void onUpdate() {
    	updatedAt = LocalDateTime.now();
    }
	
}
