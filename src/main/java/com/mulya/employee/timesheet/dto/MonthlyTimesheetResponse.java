package com.mulya.employee.timesheet.dto;
import java.util.List;

public class MonthlyTimesheetResponse {
    private List<TimesheetResponse> timesheets;
    private Double totalWorkingHours;

    public List<TimesheetResponse> getTimesheets() { return timesheets; }
    public void setTimesheets(List<TimesheetResponse> timesheets) { this.timesheets = timesheets; }

    public Double getTotalWorkingHours() { return totalWorkingHours; }
    public void setTotalWorkingHours(Double totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }
}
