package com.mulya.employee.timesheet.service;

import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.TimesheetEntry;
import com.mulya.employee.timesheet.dto.TimesheetRequest;
import com.mulya.employee.timesheet.dto.UserDto;
import com.mulya.employee.timesheet.exception.ValidationException;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.TimesheetRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final UserRegisterClient userRegisterClient;

    public TimesheetService(TimesheetRepository timesheetRepository, UserRegisterClient userRegisterClient) {
        this.timesheetRepository = timesheetRepository;
        this.userRegisterClient = userRegisterClient;
    }

    public Timesheet createTimesheet(String userId, TimesheetRequest req) {
        Map<String, String> errors = new HashMap<>();

        // Fetch minimal user info from User Register Service
        UserDto user = userRegisterClient.getUserById(userId);
        if (user == null) {
            errors.put("userId", "User not found with id: " + userId);
            throw new ValidationException(errors);
        }

        // Determine and set employeeType based on the submitted timesheet type
        String employeeType;
        if (req.getType() == TimesheetType.DAILY) {
            employeeType = "INTERNAL";
        } else if (req.getType() == TimesheetType.WEEKLY) {
            employeeType = "EXTERNAL";
        } else {
            errors.put("timesheetType", "Invalid timesheet type");
            throw new ValidationException(errors);
        }

        user.setEmployeeType(employeeType); // Update DTO field if needed

        // Validate timesheet submission consistency according to employeeType
        if ("INTERNAL".equalsIgnoreCase(employeeType)) {
            if (req.getType() != TimesheetType.DAILY) {
                errors.put("timesheetType", "Internal employees must submit DAILY timesheets");
            }
        } else if ("EXTERNAL".equalsIgnoreCase(employeeType)) {
            if (req.getType() != TimesheetType.WEEKLY) {
                errors.put("timesheetType", "External employees must submit WEEKLY timesheets");
            }
            // Weekly timesheets must start on Monday
            if (req.getType() == TimesheetType.WEEKLY && req.getDate().getDayOfWeek() != DayOfWeek.MONDAY) {
                errors.put("date", "Weekly timesheets must start on a Monday");
            }
        } else {
            errors.put("userType", "Unrecognized employee type: " + employeeType);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Calculate total hours from entries list
        double totalHours = calculateTotalHours(req.getEntries());

        double targetHours = (req.getType() == TimesheetType.DAILY) ? 8.0 : 8.0 * 5.0;
        double percentage = (totalHours / targetHours) * 100;

        Timesheet entity = new Timesheet();
        entity.setUserId(userId);
        entity.setTimesheetType(req.getType());
        entity.setTimesheetDate(req.getDate());

        try {
            entity.setEntries(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req.getEntries()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize timesheet entries: " + e.getMessage());
        }

        entity.setPercentageOfTarget(percentage);

        // Set the employeeType field in the Timesheet entity as well
        entity.setEmployeeType(employeeType);

        return timesheetRepository.save(entity);
    }

    private double calculateTotalHours(List<TimesheetEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Timesheet entries cannot be empty");
        }
        return entries.stream()
                .mapToDouble(TimesheetEntry::getHours)
                .sum();
    }

    public List<Timesheet> getTimesheetsByUserId(String userId) {
        return timesheetRepository.findByUserId(userId);
    }
}
