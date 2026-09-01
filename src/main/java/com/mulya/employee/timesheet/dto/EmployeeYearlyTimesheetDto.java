package com.mulya.employee.timesheet.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeYearlyTimesheetDto {
    private String candidateId;
    private String employeeId;
    private String candidateName;
    private String employmentType;
    private String vendor;
    private String client;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Integer> monthlyHours = new ArrayList<>();
    private int totalHours;

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<Integer> getMonthlyHours() {
        return monthlyHours;
    }

    public void setMonthlyHours(List<Integer> monthlyHours) {
        this.monthlyHours = monthlyHours;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }
}
