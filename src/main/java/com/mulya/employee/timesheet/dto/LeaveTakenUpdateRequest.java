package com.mulya.employee.timesheet.dto;

public class LeaveTakenUpdateRequest {
    private int leavesTakenNow;
    private String updatedBy;

    // Getters and setters
    public int getLeavesTakenNow() { return leavesTakenNow; }
    public void setLeavesTakenNow(int leavesTakenNow) { this.leavesTakenNow = leavesTakenNow; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
