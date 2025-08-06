package com.mulya.employee.timesheet.contoller;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.dto.TimesheetEntry;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mulya.employee.timesheet.dto.ApiResponse;
import com.mulya.employee.timesheet.dto.TimesheetRequest;
import com.mulya.employee.timesheet.dto.TimesheetResponse;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/timesheet")
@Tag(name = "Timesheet API", description = "Endpoints to submit and manage timesheets")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @Operation(summary = "Submit timesheet")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<TimesheetResponse>> submitTimesheet(
            @RequestParam String userId,
            @Valid @RequestBody TimesheetRequest request) {
        Timesheet saved = timesheetService.createTimesheet(userId, request);
        TimesheetResponse response = mapToResponse(saved);
        return ResponseEntity.ok(ApiResponse.success("Timesheet submitted successfully", response));
    }

    @Operation(summary = "Get timesheets by user ID")
    @GetMapping("/getTimesheetsByUserId")
    public ResponseEntity<ApiResponse<List<TimesheetResponse>>> getUserTimesheets(@RequestParam String userId) {
        List<Timesheet> timesheets = timesheetService.getTimesheetsByUserId(userId);
        List<TimesheetResponse> responses = timesheets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Timesheets retrieved successfully", responses));
    }

    private TimesheetResponse mapToResponse(Timesheet ts) {
        TimesheetResponse resp = new TimesheetResponse();
        resp.setId(ts.getId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeType(ts.getEmployeeType());
        resp.setTimesheetType(ts.getTimesheetType());
        resp.setTimesheetDate(ts.getTimesheetDate());

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<TimesheetEntry> entries = mapper.readValue(ts.getEntries(), new TypeReference<List<TimesheetEntry>>() {});
            resp.setEntries(entries);
        } catch (Exception e) {
            // In case of any error deserializing, you can set entries as empty list or null
            resp.setEntries(List.of()); // or null if you prefer
        }

        resp.setPercentageOfTarget(ts.getPercentageOfTarget());

        return resp;
    }
}
