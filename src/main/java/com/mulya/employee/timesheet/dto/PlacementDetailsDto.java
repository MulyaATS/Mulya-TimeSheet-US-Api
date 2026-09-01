package com.mulya.employee.timesheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementDetailsDto {
    private LocalDate startDate;
    private String clientName;
    @JsonProperty("employmentType")
    private String employeeType;
    @JsonProperty("vendorName")
    private String vendorName;
    private String employeeWorkingType;
    @JsonProperty("candidateId")
    private String candidateId;
    @JsonProperty("id")
    private String id;
    private LocalDate endDate;
    @JsonProperty("candidateEmailId")
    private String candidateEmail;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("candidateFullName")
    private String candidateFullName;

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getEmployeeWorkingType() {
        return employeeWorkingType;
    }

    public void setEmployeeWorkingType(String employeeWorkingType) {
        this.employeeWorkingType = employeeWorkingType;
    }

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

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }
}
