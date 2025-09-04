package com.mulya.employee.timesheet.contoller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.*;
import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.AttachmentRepository;
import com.mulya.employee.timesheet.service.LeaveService;
import com.mulya.employee.timesheet.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/timesheet")
@Tag(name = "Timesheet API")
@CrossOrigin
public class TimesheetController {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TimesheetService timesheetService;

    @Autowired
    private UserRegisterClient userRegisterClient;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/daily-entry")
    public ResponseEntity<ApiResponse<TimesheetSummaryDto>> saveDailyEntry(
            @RequestParam String userId,
            @Valid @RequestBody TimesheetRequest request) {
        Timesheet ts = timesheetService.createTimesheet(userId, request);
        TimesheetSummaryDto summaryDto = timesheetService.toSummaryDto(ts);
        return ResponseEntity.ok(ApiResponse.success("Entry saved", summaryDto));
    }

    @PostMapping("/submit-weekly")
    public ResponseEntity<ApiResponse<TimesheetApprovalDto>> submitWeekly(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        Timesheet ts = timesheetService.submitWeekly(userId, weekStart);

        UserDto managerDto = userRegisterClient.getUsersByRole("ACCOUNTS")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ACCOUNTS manager"));

        TimesheetApprovalDto approvalDto = timesheetService.toApprovalDto(ts, managerDto.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Submitted for approval", approvalDto));
    }


    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<TimesheetApprovalDto>> approve(
            @RequestParam String timesheetId,
            @RequestParam String userId) {  // manager's own user ID
        Timesheet ts = timesheetService.approveTimesheet(timesheetId, userId);

        TimesheetApprovalDto approvalDto = timesheetService.toApprovalDto(ts, userId);

        return ResponseEntity.ok(ApiResponse.success("Timesheet approved", approvalDto));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<TimesheetApprovalDto>> reject(
            @RequestParam String timesheetId,
            @RequestParam String userId,
            @RequestParam String reason) {
        Timesheet ts = timesheetService.rejectTimesheet(timesheetId, userId, reason);

        TimesheetApprovalDto approvalDto = timesheetService.toApprovalDto(ts, userId);

        return ResponseEntity.ok(ApiResponse.success("Timesheet rejected", approvalDto));
    }

    @GetMapping("/timesheets-pending")
    public ResponseEntity<ApiResponse<List<TimesheetApprovalDto>>> getPendingTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managerUserId) {

        Pageable pageable = PageRequest.of(page, size);

        if (managerUserId == null) {
            managerUserId = timesheetService.getDefaultManagerUserId();
        }

        var dtosPage = timesheetService.getTimesheetsByStatus("PENDING_APPROVAL", managerUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending timesheets retrieved", dtosPage.getContent()));
    }

    @GetMapping("/timesheets-approved")
    public ResponseEntity<ApiResponse<List<TimesheetApprovalDto>>> getApprovedTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managerUserId) {

        Pageable pageable = PageRequest.of(page, size);

        if (managerUserId == null) {
            managerUserId = timesheetService.getDefaultManagerUserId();
        }

        var dtosPage = timesheetService.getTimesheetsByStatus("APPROVED", managerUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Approved timesheets retrieved", dtosPage.getContent()));
    }

    @GetMapping("/timesheets-rejected")
    public ResponseEntity<ApiResponse<List<TimesheetApprovalDto>>> getRejectedTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managerUserId) {

        Pageable pageable = PageRequest.of(page, size);

        if (managerUserId == null) {
            managerUserId = timesheetService.getDefaultManagerUserId();
        }

        var dtosPage = timesheetService.getTimesheetsByStatus("REJECTED", managerUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Rejected timesheets retrieved", dtosPage.getContent()));
    }


    @GetMapping("/getTimesheetsByUserId")
    public ResponseEntity<ApiResponse<MonthlyTimesheetResponse>> getUserTimesheets(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthEnd)
    {
        MonthlyTimesheetResponse monthlyResponse = timesheetService.getTimesheetsByUserIdAndMonth(userId, monthStart, monthEnd);
        return ResponseEntity.ok(ApiResponse.success("Timesheets retrieved", monthlyResponse));
    }


    @GetMapping("/getAllTimesheets")
    public ResponseEntity<ApiResponse<List<TimesheetResponse>>> getAllTimesheets() {
        List<TimesheetResponse> responses = timesheetService.getAllTimesheets();
        return ResponseEntity.ok(ApiResponse.success("All timesheets retrieved", responses));
    }

    @PostMapping("/{timesheetId}/attachments")
    public ResponseEntity<ApiResponse<List<AttachmentUploadResponse>>> uploadTimesheetAttachments(
            @PathVariable String timesheetId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        Timesheet ts = timesheetService.uploadAttachments(timesheetId, files);

        // You probably have a User service — fetching employee name
        String employeeName = userRegisterClient.getUserInfos(ts.getUserId()).get(0).getUserName();

        List<AttachmentUploadResponse> responses = ts.getAttachments().stream()
                .map(att -> new AttachmentUploadResponse(
                        att.getId(),
                        ts.getUserId(),
                        employeeName,
                        ts.getWeekStartDate(),
                        ts.getWeekEndDate()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "Attachments uploaded successfully",
                responses
        ));
    }

    // === LIST ATTACHMENTS ===
    @GetMapping("/{timesheetId}/attachments")
    public ResponseEntity<ApiResponse<List<AttachmentDto>>> listAttachments(@PathVariable String timesheetId) {
        List<AttachmentDto> attachments = attachmentRepository.findByTimesheetTimesheetId(timesheetId)  // Note updated method
                .stream()
                .map(att -> new AttachmentDto(
                        att.getId(),
                        att.getFilename(),
                        att.getFiletype(),
                        att.getUploadedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                "Attachments retrieved successfully",
                attachments
        ));
    }

    // === DOWNLOAD ATTACHMENT ===
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(att.getFiletype()))
                .body(att.getContent());
    }

    @DeleteMapping("/delete-attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long attachmentId) {
        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        attachmentRepository.delete(att);

        return ResponseEntity.ok(ApiResponse.success(
                "Attachment deleted successfully",
                null
        ));
    }
    @PatchMapping("/update-timesheet-entries/{id}")
    public ResponseEntity<ApiResponse<TimesheetSummaryDto>> updateTimesheetEntries(
            @PathVariable Long id,
            @RequestParam String userId,
            @Valid @RequestBody TimesheetEntriesUpdateRequest updatedEntriesRequest) throws Exception {

        Timesheet updated = timesheetService.updateTimesheetEntries(id, userId, updatedEntriesRequest.getWorkingEntries(),updatedEntriesRequest.getNonWorkingEntries());
        TimesheetSummaryDto summaryDto = timesheetService.toSummaryDto(updated);
        return ResponseEntity.ok(ApiResponse.success("Entries updated successfully", summaryDto));
    }

    @PutMapping("/update-timesheet/{id}")
    public ResponseEntity<ApiResponse<TimesheetSummaryDto>> updateWeeklyTimesheet(
            @PathVariable Long id,
            @RequestParam String userId,
            @Valid @RequestBody TimesheetRequest request) throws Exception {

        Timesheet updated = timesheetService.updateTimesheet(id, userId, request);
        TimesheetSummaryDto summaryDto = timesheetService.toSummaryDto(updated);
        return ResponseEntity.ok(ApiResponse.success("Weekly timesheet updated successfully", summaryDto));
    }

    @GetMapping("/monthly-timesheets")
    public ResponseEntity<ApiResponse<List<EmployeeMonthlyTimesheetDto>>> getAllEmployeeMonthlySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthEnd
    ) throws Exception {

        LocalDate normalizedStart = monthStart.withDayOfMonth(1);
        LocalDate normalizedEnd = (monthEnd == null) ? normalizedStart.withDayOfMonth(normalizedStart.lengthOfMonth())
                : monthEnd.withDayOfMonth(monthEnd.lengthOfMonth());

        List<EmployeeMonthlyTimesheetDto> summaries = timesheetService.getAllEmployeesMonthlySummary(normalizedStart, normalizedEnd);

        return ResponseEntity.ok(ApiResponse.success("Monthly timesheet summaries fetched", summaries));
    }

    @PostMapping("/leave-initialization")
    public ResponseEntity<ApiResponse<EmployeeLeaveSummaryDto>> initializeLeave(@RequestBody EmployeeLeaveSummaryDto dto) {
        try {
            EmployeeLeaveSummaryDto savedDto = leaveService.initializeLeaveSummaryForNewEmployee(dto);
            return ResponseEntity.ok(ApiResponse.success("Leave initialized successfully", savedDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Leave initialization failed", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), e.getMessage()));
        }
    }

    @PatchMapping("/{userId}/update-leave-taken")
    public ResponseEntity<ApiResponse<EmployeeLeaveSummaryDto>> updateLeaveTaken(
            @PathVariable String userId,
            @RequestBody LeaveTakenUpdateRequest request) {
        try {
            EmployeeLeaveSummaryDto updatedSummary = leaveService.updateLeaveOnLeaveTaken(
                    userId, request.getLeavesTakenNow(), request.getUpdatedBy());
            return ResponseEntity.ok(ApiResponse.success("Leave updated successfully", updatedSummary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update leave", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), e.getMessage()));
        }
    }

    @GetMapping("/vendors/{userId}")
    public ResponseEntity<ApiResponse<List<String>>> getVendorsForUser(@PathVariable String userId) {
        try {
            List<String> vendorNames = timesheetService.getVendorNamesByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success("Vendor names fetched successfully", vendorNames));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch vendor names", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), e.getMessage()));
        }
    }
}
