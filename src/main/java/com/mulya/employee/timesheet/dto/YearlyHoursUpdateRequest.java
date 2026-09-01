package com.mulya.employee.timesheet.dto;

import java.util.List;

public class YearlyHoursUpdateRequest {
    private String employeeId;
    private Integer year;
    private List<Integer> monthlyHours;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<Integer> getMonthlyHours() {
        return monthlyHours;
    }

    public void setMonthlyHours(List<Integer> monthlyHours) {
        this.monthlyHours = monthlyHours;
    }
}
