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
    private Double percentageOfTarget;

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

    public List<TimesheetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<TimesheetEntry> entries) {
        this.entries = entries;
    }

    public LocalDate getTimesheetDate() {
        return timesheetDate;
    }

    public void setTimesheetDate(LocalDate timesheetDate) {
        this.timesheetDate = timesheetDate;
    }

    public Double getPercentageOfTarget() {
        return percentageOfTarget;
    }

    public void setPercentageOfTarget(Double percentageOfTarget) {
        this.percentageOfTarget = percentageOfTarget;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }
}
