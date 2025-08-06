package com.mulya.employee.timesheet.dto;

public class TimesheetEntry {
    private String project;
    private double hours;
    private String description;
    private String day;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public double getHours() {
        return hours;
    }

    public void setHours(double hours) {
        this.hours = hours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
