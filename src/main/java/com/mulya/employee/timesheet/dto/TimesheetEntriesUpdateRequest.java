package com.mulya.employee.timesheet.dto;

import jakarta.validation.Valid;

import java.util.List;

public class TimesheetEntriesUpdateRequest {
    @Valid
    private List<TimesheetEntry> workingEntries;

    @Valid
    private List<TimesheetEntry> nonWorkingEntries;

    public List<TimesheetEntry> getWorkingEntries() {
        return workingEntries;
    }

    public void setWorkingEntries(List<TimesheetEntry> workingEntries) {
        this.workingEntries = workingEntries;
    }

    public List<TimesheetEntry> getNonWorkingEntries() {
        return nonWorkingEntries;
    }

    public void setNonWorkingEntries(List<TimesheetEntry> nonWorkingEntries) {
        this.nonWorkingEntries = nonWorkingEntries;
    }
}
