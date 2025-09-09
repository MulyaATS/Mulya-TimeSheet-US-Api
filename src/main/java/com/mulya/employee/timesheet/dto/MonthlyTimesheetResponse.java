package com.mulya.employee.timesheet.dto;
import java.time.LocalDate;
import java.util.List;

public class MonthlyTimesheetResponse {
    private List<TimesheetResponse> timesheets;
    private Double totalWorkingHours;
    private LocalDate monthStartDate;
    private LocalDate monthEndDate;

    public List<TimesheetResponse> getTimesheets() { return timesheets; }
    public void setTimesheets(List<TimesheetResponse> timesheets) { this.timesheets = timesheets; }

    public Double getTotalWorkingHours() { return totalWorkingHours; }
    public void setTotalWorkingHours(Double totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }

    public LocalDate getMonthStartDate(){ return monthStartDate; }
    public void setMonthStartDate(LocalDate monthStartDate){ this.monthStartDate = monthStartDate; }

    public LocalDate getMonthEndDate(){ return monthEndDate; }
    public void setMonthEndDate(LocalDate monthEndDate){ this.monthEndDate = monthEndDate; }
}
