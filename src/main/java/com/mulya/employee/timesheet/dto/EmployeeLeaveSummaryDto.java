package com.mulya.employee.timesheet.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeLeaveSummaryDto {

    private String userId;

    private String employeeName;

    private Integer availableLeaves;

    private Integer takenLeaves;

    private Integer leaveBalance;

    private String updatedBy;

    private LocalDateTime updatedAt = LocalDateTime.now();

    private String employeeType;   // Added for initialization
    private LocalDate joiningDate;

    public LocalDate getJoiningDate() {
        return joiningDate;
    }
    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }
    public String getEmployeeType() {
        return employeeType;
    }
    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
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

    public Integer getLeaveBalance() {
        return leaveBalance;
    }

    public void setLeaveBalance(Integer leaveBalance) {
        this.leaveBalance = leaveBalance;
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
