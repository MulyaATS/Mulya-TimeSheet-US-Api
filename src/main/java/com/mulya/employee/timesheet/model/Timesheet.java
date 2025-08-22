package com.mulya.employee.timesheet.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "timesheets")
public class Timesheet {

    @Id
    @Column(name = "timesheet_id", length = 12)
    private String timesheetId;   // replaces Long id field

    @Column(nullable = false)
    private String userId; // ID from User microservice

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimesheetType timesheetType; // DAILY or WEEKLY

    /**
     * For DAILY: the specific date of the timesheet
     * For WEEKLY: the Monday of the week
     */
    @Column(nullable = false)
    private LocalDate timesheetDate;

    /**
     * For weekly entries: start and end of the week
     * For daily: startDate == endDate == timesheetDate
     */
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    /**
     * JSON of all entries for this day/week
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String nonWorkingHours;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String workingHours;


    @Column(nullable = false)
    private Double percentageOfTarget; // e.g. hours / 40 * 100

    @Column(nullable = false)
    private String employeeType;


    private String status;

    private String notes;

    private String approvedBy;
    private LocalDateTime approvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments;

    public String getTimesheetId() { return timesheetId; }
    public void setTimesheetId(String timesheetId) { this.timesheetId = timesheetId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public TimesheetType getTimesheetType() { return timesheetType; }
    public void setTimesheetType(TimesheetType timesheetType) { this.timesheetType = timesheetType; }

    public LocalDate getTimesheetDate() { return timesheetDate; }
    public void setTimesheetDate(LocalDate timesheetDate) { this.timesheetDate = timesheetDate; }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getNonWorkingHours() { return nonWorkingHours; }
    public void setNonWorkingHours(String nonWorkingHours) { this.nonWorkingHours = nonWorkingHours; }

    public Double getPercentageOfTarget() { return percentageOfTarget; }
    public void setPercentageOfTarget(Double percentageOfTarget) { this.percentageOfTarget = percentageOfTarget; }

    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments;}

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;}
}
