package com.mulya.employee.timesheet.dto;

import com.mulya.employee.timesheet.model.TimesheetType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimesheetResponse {
    private String timesheetId;
    private String userId;
    private String employeeName;
    private String employeeRoleType;
    private TimesheetType timesheetType;
    private List<TimesheetEntry> workingEntries;
    private List<TimesheetEntry> nonWorkingEntries;
    private String employeeType;
    private LocalDate timesheetDate;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Double percentageOfTarget;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private List<AttachmentDto> attachments;
    private String notes;
    private String approver;


    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        if (notes != null && notes.length() > 500) {
            throw new IllegalArgumentException("Notes cannot exceed 500 characters");
        }
        this.notes = notes;
    }

    private LocalDate startDate;
    private String clientName;

    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getClientName() {
        return clientName;
    }
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getApprover() {
        return approver;
    }
    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getTimesheetId() { return timesheetId; }
    public void setTimesheetId(String timesheetId) { this.timesheetId = timesheetId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmployeeRoleType() { return employeeRoleType; }
    public void setEmployeeRoleType(String employeeType) { this.employeeRoleType = employeeType; }

    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }

    public TimesheetType getTimesheetType() { return timesheetType; }
    public void setTimesheetType(TimesheetType timesheetType) { this.timesheetType = timesheetType; }

    public List<TimesheetEntry> getWorkingEntries() { return workingEntries; }
    public void setWorkingEntries(List<TimesheetEntry> workingEntries) { this.workingEntries = workingEntries; }

    public List<TimesheetEntry> getNonWorkingEntries() { return nonWorkingEntries; }
    public void setNonWorkingEntries(List<TimesheetEntry> nonWorkingEntries) { this.nonWorkingEntries = nonWorkingEntries; }

    public LocalDate getTimesheetDate() { return timesheetDate; }
    public void setTimesheetDate(LocalDate timesheetDate) { this.timesheetDate = timesheetDate; }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }

    public Double getPercentageOfTarget() { return percentageOfTarget; }
    public void setPercentageOfTarget(Double percentageOfTarget) { this.percentageOfTarget = percentageOfTarget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }

}
