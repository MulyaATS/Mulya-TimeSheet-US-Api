package com.mulya.employee.timesheet.contoller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.*;
import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.AttachmentRepository;
import com.mulya.employee.timesheet.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
            @RequestParam Long timesheetId,
            @RequestParam String userId) {  // manager's own user ID
        Timesheet ts = timesheetService.approveTimesheet(timesheetId, userId);

        TimesheetApprovalDto approvalDto = timesheetService.toApprovalDto(ts, userId);

        return ResponseEntity.ok(ApiResponse.success("Timesheet approved", approvalDto));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<TimesheetApprovalDto>> reject(
            @RequestParam Long timesheetId,
            @RequestParam String userId,
            @RequestParam String reason) {
        Timesheet ts = timesheetService.rejectTimesheet(timesheetId, userId, reason);

        TimesheetApprovalDto approvalDto = timesheetService.toApprovalDto(ts, userId);

        return ResponseEntity.ok(ApiResponse.success("Timesheet rejected", approvalDto));
    }

    @GetMapping("/getTimesheetsByUserId")
    public ResponseEntity<ApiResponse<List<TimesheetResponse>>> getUserTimesheets(@RequestParam String userId) {
        List<TimesheetResponse> responses = timesheetService.getTimesheetsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Timesheets retrieved", responses));
    }

    @GetMapping("/getAllTimesheets")
    public ResponseEntity<ApiResponse<List<TimesheetResponse>>> getAllTimesheets() {
        List<TimesheetResponse> responses = timesheetService.getAllTimesheets();
        return ResponseEntity.ok(ApiResponse.success("All timesheets retrieved", responses));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<AttachmentUploadResponse>>> uploadTimesheetAttachments(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        Timesheet ts = timesheetService.uploadAttachments(id, files);

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

        if (request.getType() != TimesheetType.WEEKLY) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid timesheet type for this endpoint",
                    "400",
                    "Only WEEKLY timesheets can be updated via this method"
            ));
        }

        Timesheet updated = timesheetService.updateTimesheet(id, userId, request);
        TimesheetSummaryDto summaryDto = timesheetService.toSummaryDto(updated);
        return ResponseEntity.ok(ApiResponse.success("Weekly timesheet updated successfully", summaryDto));
    }
}
