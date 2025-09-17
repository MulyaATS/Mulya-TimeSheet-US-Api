package com.mulya.employee.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class EmployeeMonthlyTimesheetDto {
    private String employeeId;
    private String employeeName;
    private String employeeType;
    private String clientName;
    private LocalDate monthStartDate;
    private LocalDate monthEndDate;
    @JsonProperty("startDate")
    private LocalDate joiningDate;
    private int week1Hours;
    private int week2Hours;
    private int week3Hours;
    private int week4Hours;
    private int week5Hours;
    private int totalWorkingHours;
    private int totalWorkingDays;
    private int availableLeaves;
    private int takenLeaves;
    private String Status;

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDate getMonthStartDate() {
        return monthStartDate;
    }
    public void setMonthStartDate(LocalDate monthStartDate) {
        this.monthStartDate = monthStartDate;
    }
    public LocalDate getMonthEndDate() {
        return monthEndDate;
    }
    public void setMonthEndDate(LocalDate monthEndDate) {
        this.monthEndDate = monthEndDate;
    }

    public int getWeek1Hours() {
        return week1Hours;
    }

    public void setWeek1Hours(int week1Hours) {
        this.week1Hours = week1Hours;
    }

    public int getWeek2Hours() {
        return week2Hours;
    }

    public void setWeek2Hours(int week2Hours) {
        this.week2Hours = week2Hours;
    }

    public int getWeek3Hours() {
        return week3Hours;
    }

    public void setWeek3Hours(int week3Hours) {
        this.week3Hours = week3Hours;
    }

    public int getWeek4Hours() {
        return week4Hours;
    }

    public void setWeek4Hours(int week4Hours) {
        this.week4Hours = week4Hours;
    }

    public int getWeek5Hours() {
        return week5Hours;
    }

    public void setWeek5Hours(int week5Hours) {
        this.week5Hours = week5Hours;
    }

    public int getTotalWorkingHours() {
        return totalWorkingHours;
    }

    public void setTotalWorkingHours(int totalWorkingHours) {
        this.totalWorkingHours = totalWorkingHours;
    }

    public int getTotalWorkingDays() {
        return totalWorkingDays;
    }

    public void setTotalWorkingDays(int totalWorkingDays) {
        this.totalWorkingDays = totalWorkingDays;
    }

    public int getAvailableLeaves() {
        return availableLeaves;
    }

    public void setAvailableLeaves(int availableLeaves) {
        this.availableLeaves = availableLeaves;
    }

    public int getTakenLeaves() {
        return takenLeaves;
    }

    public void setTakenLeaves(int takenLeaves) {
        this.takenLeaves = takenLeaves;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }
    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }
}
