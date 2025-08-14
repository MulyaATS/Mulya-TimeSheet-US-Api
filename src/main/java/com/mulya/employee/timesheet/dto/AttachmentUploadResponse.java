package com.mulya.employee.timesheet.dto;

import java.time.LocalDate;

public class AttachmentUploadResponse {
    private Long attachmentId;
    private String employeeId;
    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;

    public AttachmentUploadResponse(Long attachmentId, String employeeId, String employeeName, LocalDate startDate, LocalDate endDate) {
        this.attachmentId = attachmentId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
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

    // Getters and setters...
}
