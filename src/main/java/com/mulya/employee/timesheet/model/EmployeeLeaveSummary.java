package com.mulya.employee.timesheet.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "employee_leave_summary")
public class EmployeeLeaveSummary {
    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;  // Match your timesheet userId

    @Column(nullable = true)
    private String employeeName;

    @Column(nullable = false)
    private Integer availableLeaves;

    @Column(nullable = false)
    private Integer takenLeaves;

    @Column(nullable = true)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getAvailableLeaves() {
        return availableLeaves;
    }

    public void setAvailableLeaves(Integer availableLeaves) {
        this.availableLeaves = availableLeaves;
    }

    public Integer getTakenLeaves() {
        return takenLeaves;
    }

    public void setTakenLeaves(Integer takenLeaves) {
        this.takenLeaves = takenLeaves;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
