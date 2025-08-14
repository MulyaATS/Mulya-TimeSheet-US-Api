package com.mulya.employee.timesheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.*;
import com.mulya.employee.timesheet.exception.ValidationException;
import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.AttachmentRepository;
import com.mulya.employee.timesheet.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UserRegisterClient userRegisterClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Transactional
    public Timesheet createTimesheet(String userId, TimesheetRequest req) {
        Map<String, String> errors = new HashMap<>();
        UserDto user = userRegisterClient.getUserById(userId);
        if (user == null) {
            errors.put("userId", "User not found: " + userId);
            throw new ValidationException(errors);
        }

        String employeeType = (req.getType() == TimesheetType.DAILY) ? "INTERNAL" : "EXTERNAL";

        if ("INTERNAL".equalsIgnoreCase(employeeType) && req.getType() != TimesheetType.DAILY) {
            errors.put("timesheetType", "Internal employees must submit DAILY timesheets");
        }
        if ("EXTERNAL".equalsIgnoreCase(employeeType)) {
            if (req.getType() != TimesheetType.WEEKLY) {
                errors.put("timesheetType", "External employees must submit WEEKLY");
            }
            if (req.getDate().getDayOfWeek() != DayOfWeek.MONDAY) {
                errors.put("date", "Weekly timesheets must start on a Monday");
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        if ("INTERNAL".equalsIgnoreCase(employeeType)) {
            return saveDailyRecord(userId, req, employeeType);
        }

        LocalDate weekStart = req.getDate();             // Monday
        LocalDate weekEnd = weekStart.plusDays(4);       // Friday
        Timesheet ts = timesheetRepository
                .findByUserIdAndWeekStartDate(userId, weekStart)
                .orElseGet(() -> {
                    Timesheet t = new Timesheet();
                    t.setUserId(userId);
                    t.setTimesheetType(TimesheetType.WEEKLY);
                    t.setTimesheetDate(weekStart);
                    t.setWeekStartDate(weekStart);
                    t.setWeekEndDate(weekEnd);
                    t.setEmployeeType(employeeType);
                    t.setStatus("DRAFT");
                    t.setEntries("[]");
                    t.setPercentageOfTarget(0.0);
                    return t;
                });

        // append entries (each with date field)
        List<TimesheetEntry> currentEntries;
        try {
            currentEntries = mapper.readValue(ts.getEntries(), new TypeReference<>() {});
        } catch (Exception e) {
            currentEntries = new ArrayList<>();
        }
        currentEntries.addAll(req.getEntries());

        try {
            ts.setEntries(mapper.writeValueAsString(currentEntries));
        } catch (Exception e) {
            throw new RuntimeException("Error writing entries JSON", e);
        }

        double totalHours = currentEntries.stream().mapToDouble(TimesheetEntry::getHours).sum();
        ts.setPercentageOfTarget((totalHours / 40.0) * 100);

        return timesheetRepository.save(ts);
    }

    private Timesheet saveDailyRecord(String userId, TimesheetRequest req, String employeeType) {
        double totalHours = calculateTotalHours(req.getEntries());
        Timesheet ts = new Timesheet();
        ts.setUserId(userId);
        ts.setTimesheetType(TimesheetType.DAILY);
        ts.setTimesheetDate(req.getDate());
        ts.setWeekStartDate(req.getDate());
        ts.setWeekEndDate(req.getDate());
        ts.setEmployeeType(employeeType);
        ts.setStatus("APPROVED");
        try {
            ts.setEntries(mapper.writeValueAsString(req.getEntries()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ts.setPercentageOfTarget((totalHours / 8.0) * 100);
        return timesheetRepository.save(ts);
    }

    @Transactional
    public Timesheet submitWeekly(String userId, LocalDate weekStart) {
        Timesheet ts = timesheetRepository.findByUserIdAndWeekStartDate(userId, weekStart)
                .orElseThrow(() -> new IllegalArgumentException("No timesheet found for this week"));
        ts.setStatus("PENDING_APPROVAL");

        UserDto managerDto = userRegisterClient.getUsersByRole("ACCOUNTS")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ACCOUNTS manager"));

        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerDto.getUserId()).get(0);
        UserInfoDto empInfo = userRegisterClient.getUserInfos(ts.getUserId()).get(0);

        emailService.sendManagerApprovalRequestEmail(
                ts,
                managerDto.getEmail(),
                managerInfo.getUserName(),
                empInfo.getUserName()
        );

        return timesheetRepository.save(ts);
    }

    @Transactional
    public Timesheet approveTimesheet(Long id, String managerUserId) {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        ts.setStatus("APPROVED");
        ts.setApprovedAt(LocalDateTime.now());

        // Fetch manager info
        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerUserId).get(0);
        if (managerInfo.getUserName() == null) {
            throw new IllegalStateException("Manager name not found for " + managerUserId);
        }
        ts.setApprovedBy(managerInfo.getUserName());

        // Fetch employee email
        String empEmail = userRegisterClient.getUserEmail(ts.getUserId());
        UserInfoDto empInfo = userRegisterClient.getUserInfos(ts.getUserId()).get(0);

        emailService.sendEmployeeApprovalEmail(
                empEmail,
                empInfo.getUserName(),
                ts.getWeekStartDate().toString(),
                ts.getWeekEndDate().toString()
        );

        return timesheetRepository.save(ts);
    }



    @Transactional
    public Timesheet rejectTimesheet(Long timesheetId, String managerUserId, String reason) {
        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        ts.setStatus("REJECTED");
        ts.setApprovedAt(LocalDateTime.now());

        // Lookup manager info
        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerUserId).get(0);
        ts.setApprovedBy(managerInfo.getUserName());

        // Lookup employee info
        UserInfoDto empInfo = userRegisterClient.getUserInfos(ts.getUserId()).get(0);
        String empEmail = userRegisterClient.getUserEmail(ts.getUserId());

        // Send employee rejection notification
        emailService.sendEmployeeRejectionEmail(
                empEmail,
                empInfo.getUserName(),
                ts.getWeekStartDate().toString(),
                ts.getWeekEndDate().toString(),
                reason
        );

        return timesheetRepository.save(ts);
    }

    private double calculateTotalHours(List<TimesheetEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Entries cannot be empty");
        }
        return entries.stream().mapToDouble(TimesheetEntry::getHours).sum();
    }

    public List<TimesheetResponse> getTimesheetsByUserId(String userId) {
        return timesheetRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse) // map entity -> DTO
                .collect(Collectors.toList());
    }

    private TimesheetResponse mapToResponse(Timesheet ts) {
        TimesheetResponse resp = new TimesheetResponse();
        resp.setId(ts.getId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeType(ts.getEmployeeType());
        resp.setTimesheetType(ts.getTimesheetType());
        resp.setTimesheetDate(ts.getTimesheetDate());
        resp.setWeekStartDate(ts.getWeekStartDate());
        resp.setWeekEndDate(ts.getWeekEndDate());

        try {
            resp.setEntries(mapper.readValue(
                    ts.getEntries(),
                    new TypeReference<List<TimesheetEntry>>() {}
            ));
        } catch (Exception e) {
            System.err.println("❌ Failed to parse 'entries' JSON for Timesheet ID: " + ts.getId());
            System.err.println("Raw DB value: " + ts.getEntries());
            e.printStackTrace();
            resp.setEntries(List.of()); // safe fallback
        }

        resp.setPercentageOfTarget(ts.getPercentageOfTarget());
        resp.setStatus(ts.getStatus());
        resp.setApprovedBy(ts.getApprovedBy());
        resp.setApprovedAt(ts.getApprovedAt());

        // ✅ Map Attachments from entity -> DTO
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


    public Timesheet updateTimesheet(Long id, String userId, TimesheetRequest req) throws Exception {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));
        ts.setTimesheetType(req.getType());
        ts.setTimesheetDate(req.getDate());
        ts.setEntries(new ObjectMapper().writeValueAsString(req.getEntries()));
        double target = (req.getType() == TimesheetType.DAILY) ? 8.0 : 40.0;
        ts.setPercentageOfTarget((calculateTotalHours(req.getEntries()) / target) * 100);
        return timesheetRepository.save(ts);
    }

    @Transactional
    public Timesheet uploadAttachments(Long timesheetId, List<MultipartFile> files) throws IOException {
        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        // ✅ Check if attachments already exist for this week
        boolean exists = !ts.getAttachments().isEmpty();
        if (exists) {
            throw new IllegalStateException(
                    String.format("Attachment already exists for week %s to %s",
                            ts.getWeekStartDate(), ts.getWeekEndDate())
            );
        }

        for (MultipartFile file : files) {
            Attachment attachment = new Attachment();
            attachment.setTimesheet(ts);
            attachment.setFilename(file.getOriginalFilename());
            attachment.setFiletype(file.getContentType());
            attachment.setContent(file.getBytes());
            attachment.setWeekStartDate(ts.getWeekStartDate());
            attachment.setWeekEndDate(ts.getWeekEndDate());
            ts.getAttachments().add(attachment);
        }

        return timesheetRepository.save(ts);
    }


    public void deleteTimesheet(Long id) {
        if (!timesheetRepository.existsById(id)) {
            throw new IllegalArgumentException("Timesheet not found");
        }
        timesheetRepository.deleteById(id);
    }
}
