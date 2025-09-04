package com.mulya.employee.timesheet.dto;

import com.mulya.employee.timesheet.model.TimesheetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public class TimesheetRequest {

    @NotNull(message = "Date must be provided")
    private LocalDate date;

    @NotNull(message = "Entries JSON cannot be null")
    private List<TimesheetEntry> workingEntries;

    @NotNull(message = "Entries JSON cannot be null")
    private List<TimesheetEntry> nonWorkingEntries;

    private String notes;
    // Getters and setters

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        if (notes != null && notes.length() > 500) {
            throw new IllegalArgumentException("Notes cannot exceed 500 characters");
        }
        this.notes = notes;
    }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<TimesheetEntry> getWorkingEntries() { return workingEntries; }

    public void setWorkingEntries(List<TimesheetEntry> workingEntries) {
        this.workingEntries = workingEntries;
    }
    public List<TimesheetEntry> getNonWorkingEntries() {
        return nonWorkingEntries;
    }
    public void setNonWorkingEntries(List<TimesheetEntry> nonWorkingEntries) {
        this.nonWorkingEntries = nonWorkingEntries;}
}
