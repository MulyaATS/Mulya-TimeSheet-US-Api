package com.mulya.employee.timesheet.dto;
import java.time.LocalDate;
import java.util.List;

public class Week {
    public LocalDate weekStart;  // Monday
    public LocalDate weekEnd;    // Friday
    public List<LocalDate> daysInsideMonth;

    public Week(LocalDate start, LocalDate end, List<LocalDate> days) {
        this.weekStart = start;
        this.weekEnd = end;
        this.daysInsideMonth = days;
    }
}