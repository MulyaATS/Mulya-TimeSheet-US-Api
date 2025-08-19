package com.mulya.employee.timesheet.dto;

import com.mulya.employee.timesheet.model.TimesheetType;

import java.time.LocalDate;

public class TimesheetSummaryDto {
    private Long id;
    private String userId;
    private String employeeName;
    private String employeeType;
    private TimesheetType timesheetType;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String status;

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

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }

    public TimesheetType getTimesheetType() {
        return timesheetType;
    }

    public void setTimesheetType(TimesheetType timesheetType) {
        this.timesheetType = timesheetType;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
