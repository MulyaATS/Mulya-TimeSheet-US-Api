package com.mulya.employee.timesheet.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "timesheets")
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimesheetType timesheetType;

    @Column(nullable = false)
    private LocalDate timesheetDate; // For daily: date, for weekly: week start

    @Column(length = 5000)
    private String entries;

    @Column(nullable = false)
    private Double percentageOfTarget;


    @Column(nullable = false)
    private String employeeType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TimesheetType getTimesheetType() {
        return timesheetType;
    }

    public void setTimesheetType(TimesheetType timesheetType) {
        this.timesheetType = timesheetType;
    }

    public LocalDate getTimesheetDate() {
        return timesheetDate;
    }

    public void setTimesheetDate(LocalDate timesheetDate) {
        this.timesheetDate = timesheetDate;
    }

    public String getEntries() {
        return entries;
    }

    public void setEntries(String entries) {
        this.entries = entries;
    }

    public Double getPercentageOfTarget() {
        return percentageOfTarget;
    }

    public void setPercentageOfTarget(Double percentageOfTarget) {
        this.percentageOfTarget = percentageOfTarget;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }
}