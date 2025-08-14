package com.mulya.employee.timesheet.dto;

import com.mulya.employee.timesheet.model.TimesheetType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimesheetResponse {
    private Long id;
    private String userId;
    private String employeeType;
    private TimesheetType timesheetType;
    private List<TimesheetEntry> entries;
    private LocalDate timesheetDate;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Double percentageOfTarget;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private List<AttachmentDto> attachments;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmployeeType() { return employeeType; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }

    public TimesheetType getTimesheetType() { return timesheetType; }
    public void setTimesheetType(TimesheetType timesheetType) { this.timesheetType = timesheetType; }

    public List<TimesheetEntry> getEntries() { return entries; }
    public void setEntries(List<TimesheetEntry> entries) { this.entries = entries; }

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
