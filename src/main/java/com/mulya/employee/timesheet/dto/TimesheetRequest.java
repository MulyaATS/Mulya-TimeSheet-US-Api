package com.mulya.employee.timesheet.dto;

import com.mulya.employee.timesheet.model.TimesheetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public class TimesheetRequest {

    @NotNull(message = "Timesheet type must be provided")
    private TimesheetType type;

    @NotNull(message = "Date must be provided")
    private LocalDate date;

    @NotNull(message = "Entries JSON cannot be null")
    private List<TimesheetEntry> entries;
    // Getters and setters
    public TimesheetType getType() { return type; }
    public void setType(TimesheetType type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<TimesheetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<TimesheetEntry> entries) {
        this.entries = entries;
    }
}
