package com.mulya.employee.timesheet.dto;

import java.time.LocalDate;

public class TimesheetApprovalDto {
    private String timesheetId;
    private String userId;          // Employee userId
    private String employeeName;
    private String approveId;       // Manager userId
    private String approvedBy;      // Manager name
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    public String getTimesheetId() {
        return timesheetId;
    }
    public void setTimesheetId(String timesheetId) {
        this.timesheetId = timesheetId;
    }

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

    public String getApproveId() {
        return approveId;
    }

    public void setApproveId(String approveId) {
        this.approveId = approveId;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    // Getters and setters
}
