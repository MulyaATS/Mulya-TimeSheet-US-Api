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
    public ResponseEntity<ApiResponse<TimesheetResponse>> saveDailyEntry(
            @RequestParam String userId,
            @Valid @RequestBody TimesheetRequest request) {
        Timesheet ts = timesheetService.createTimesheet(userId, request);
        TimesheetResponse resp = new TimesheetResponse();
        resp.setId(ts.getId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeType(ts.getEmployeeType());
        resp.setTimesheetType(ts.getTimesheetType());
        resp.setStatus(ts.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Entry saved", resp));
    }

    @PostMapping("/submit-weekly")
    public ResponseEntity<ApiResponse<TimesheetResponse>> submitWeekly(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        Timesheet ts = timesheetService.submitWeekly(userId, weekStart);
        return ResponseEntity.ok(ApiResponse.success("Submitted for approval", map(ts)));
    }

    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<TimesheetResponse>> approve(
            @RequestParam Long timesheetId,
            @RequestParam String userId) { // <- This is the manager's own user ID
        Timesheet ts = timesheetService.approveTimesheet(timesheetId, userId);
        return ResponseEntity.ok(ApiResponse.success("Timesheet approved", map(ts)));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<TimesheetResponse>> reject(
            @RequestParam Long timesheetId,
            @RequestParam String userId,   // Manager's own userId
            @RequestParam String reason) {

        Timesheet ts = timesheetService.rejectTimesheet(timesheetId, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Timesheet rejected", map(ts)));
    }

    @GetMapping("/getTimesheetsByUserId")
    public ResponseEntity<ApiResponse<List<TimesheetResponse>>> getUserTimesheets(@RequestParam String userId) {
        List<TimesheetResponse> responses = timesheetService.getTimesheetsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Timesheets retrieved", responses));
    }

    private TimesheetResponse map(Timesheet ts) {
        TimesheetResponse resp = new TimesheetResponse();
        resp.setId(ts.getId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeType(ts.getEmployeeType());
        resp.setTimesheetType(ts.getTimesheetType());
        resp.setTimesheetDate(ts.getTimesheetDate());
        resp.setWeekStartDate(ts.getWeekStartDate());
        resp.setWeekEndDate(ts.getWeekEndDate());

        try {
            resp.setEntries(mapper.readValue(ts.getEntries(), new TypeReference<List<TimesheetEntry>>() {}));
        } catch (Exception e) {
            // Log detailed error for debugging
            System.err.println("❌ Failed to parse 'entries' JSON for Timesheet ID: " + ts.getId());
            System.err.println("Raw DB value: " + ts.getEntries());
            e.printStackTrace();
            resp.setEntries(List.of()); // return empty list to avoid null issues
        }

        resp.setPercentageOfTarget(ts.getPercentageOfTarget());
        resp.setStatus(ts.getStatus());
        resp.setApprovedBy(ts.getApprovedBy());
        resp.setApprovedAt(ts.getApprovedAt());
        if (ts.getAttachments() != null) {
            List<AttachmentDto> attachmentDtos = ts.getAttachments()
                    .stream()
                    .map(att -> new AttachmentDto(
                            att.getId(),
                            att.getFilename(),
                            att.getFiletype(),
                            att.getUploadedAt()
                    ))
                    .collect(Collectors.toList());
            resp.setAttachments(attachmentDtos);
        } else {
            resp.setAttachments(List.of());
        }
        return resp;
    }

    // === UPLOAD ATTACHMENTS ===
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
    @GetMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<AttachmentDto>>> listAttachments(@PathVariable Long id) {
        List<AttachmentDto> attachments = attachmentRepository.findByTimesheetId(id)
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
    public ResponseEntity<ApiResponse<TimesheetResponse>> updateTimesheetEntries(
            @PathVariable Long id,
            @RequestParam String userId,
            @Valid @RequestBody List<TimesheetEntry> updatedEntries) throws Exception {

        Timesheet updated = timesheetService.updateTimesheetEntries(id, userId, updatedEntries);
        return ResponseEntity.ok(ApiResponse.success(
                "Entries updated successfully",
                map(updated)
        ));
    }


    @PutMapping("update-timesheet/{id}")
    public ResponseEntity<ApiResponse<TimesheetResponse>> updateWeeklyTimesheet(
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
        return ResponseEntity.ok(ApiResponse.success(
                "Weekly timesheet updated successfully",
                map(updated)  // your existing mapping method
        ));
    }


}
