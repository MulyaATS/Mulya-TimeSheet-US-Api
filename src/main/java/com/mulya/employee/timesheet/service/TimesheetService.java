package com.mulya.employee.timesheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.client.CandidateClient;
import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.*;
import com.mulya.employee.timesheet.exception.ResourceNotFoundException;
import com.mulya.employee.timesheet.exception.ValidationException;
import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.AttachmentRepository;
import com.mulya.employee.timesheet.repository.TimesheetRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

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
    private CandidateClient candidateClient;

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

        String note = req.getNotes();

        LocalDate weekStart = req.getDate();
        LocalDate weekEnd = weekStart.plusDays(4);
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
                    t.setWorkingHours("[]");
                    t.setNonWorkingHours("[]");
                    t.setPercentageOfTarget(0.0);
                    t.setNotes(note);
                    String newTimesheetId = generateNextTimesheetId();
                    t.setTimesheetId(newTimesheetId);
                    return t;
                });

        List<TimesheetEntry> currentWorkingHours;
        List<TimesheetEntry> currentNonWorkingHours;
        try {
            currentWorkingHours = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentWorkingHours = new ArrayList<>();
        }try {
            currentNonWorkingHours = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentNonWorkingHours = new ArrayList<>();
        }
        List<TimesheetEntry> newWorkingEntries = req.getWorkingEntries();
        List<TimesheetEntry> newNonWorkingEntries = req.getNonWorkingEntries();

        // Check for duplicate dates
        Set<LocalDate> existingWorkingDates = currentWorkingHours.stream()
                .map(TimesheetEntry::getDate)
                .collect(Collectors.toSet());

        for (TimesheetEntry newEntry : newWorkingEntries) {
            if (existingWorkingDates.contains(newEntry.getDate())) {
                throw new IllegalArgumentException("Duplicate working hour entry for date: " + newEntry.getDate());
            }
        }

        currentWorkingHours.addAll(newWorkingEntries);
        currentNonWorkingHours.addAll(newNonWorkingEntries);

        try {
            ts.setWorkingHours(mapper.writeValueAsString(currentWorkingHours));
            ts.setNonWorkingHours(mapper.writeValueAsString(currentNonWorkingHours));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing working/non-working hours JSON", e);
        }

        // Calculate total working hours for percentageOfTarget
        double totalWorkingHours = currentWorkingHours.stream().mapToDouble(TimesheetEntry::getHours).sum();
        ts.setPercentageOfTarget((totalWorkingHours / 40.0) * 100);

        return timesheetRepository.save(ts);
    }

    public String generateNextTimesheetId() {
        // Query the max existing timesheetId from DB
        String maxId = timesheetRepository.findMaxTimesheetId();

        if (maxId == null) {
            // No existing ID, start from TMST00000001
            return "TMST00000001";
        }

        // Extract numeric part (last 8 characters of the ID string)
        int num = Integer.parseInt(maxId.substring(4));
        num++;

        // Return formatted string with prefix TMST and zero-padded number
        return String.format("TMST%08d", num);
    }


    private Timesheet saveDailyRecord(String userId, TimesheetRequest req, String employeeType) {
        double totalWorkingHours = calculateTotalHours(req.getWorkingEntries());

        Timesheet ts = new Timesheet();
        ts.setUserId(userId);
        ts.setTimesheetType(TimesheetType.DAILY);
        ts.setTimesheetDate(req.getDate());
        ts.setWeekStartDate(req.getDate());
        ts.setWeekEndDate(req.getDate());
        ts.setEmployeeType(employeeType);
        ts.setStatus("APPROVED");

        try {
            // Serialize working and non-working entries separately
            ts.setWorkingHours(mapper.writeValueAsString(req.getWorkingEntries()));
            ts.setNonWorkingHours(mapper.writeValueAsString(req.getNonWorkingEntries()));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing working/non-working entries JSON", e);
        }

        ts.setPercentageOfTarget((totalWorkingHours / 8.0) * 100);
        ts.setNotes(req.getNotes());

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
    public TimesheetSummaryDto toSummaryDto(Timesheet ts) {
        TimesheetSummaryDto dto = new TimesheetSummaryDto();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setUserId(ts.getUserId());
        dto.setEmployeeType(ts.getEmployeeType());
        dto.setTimesheetType(ts.getTimesheetType());
        dto.setWeekStartDate(ts.getWeekStartDate());
        dto.setWeekEndDate(ts.getWeekEndDate());
        dto.setStatus(ts.getStatus());

        List<UserInfoDto> userInfoList = userRegisterClient.getUserInfos(ts.getUserId());
        dto.setEmployeeName(userInfoList.isEmpty() ? "Unknown" : userInfoList.get(0).getUserName());

        return dto;
    }

    public TimesheetApprovalDto toApprovalDto(Timesheet ts, String managerUserId) {
        TimesheetApprovalDto dto = new TimesheetApprovalDto();
        dto.setTimesheetId(ts.getTimesheetId());
        dto.setUserId(ts.getUserId());

        List<UserInfoDto> userInfoList = userRegisterClient.getUserInfos(ts.getUserId());
        dto.setEmployeeName(userInfoList.isEmpty() ? "Unknown" : userInfoList.get(0).getUserName());

        dto.setApproveId(managerUserId);
        List<UserInfoDto> managerInfoList = userRegisterClient.getUserInfos(managerUserId);
        dto.setApprovedBy(managerInfoList.isEmpty() ? "Unknown" : managerInfoList.get(0).getUserName());

        dto.setWeekStartDate(ts.getWeekStartDate());
        dto.setWeekEndDate(ts.getWeekEndDate());

        return dto;
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

    public List<TimesheetResponse> getAllTimesheets() {
        return timesheetRepository.findAll()
                .stream()
                .map(this::mapToResponse) // map entity -> DTO
                .collect(Collectors.toList());
    }

    private TimesheetResponse mapToResponse(Timesheet ts) {
        System.out.println("Mapping Timesheet ID: " + ts.getTimesheetId() + ", User ID: " + ts.getUserId());

        List<UserInfoDto> userInfos = userRegisterClient.getUserInfos(ts.getUserId());
        String employeeName = userInfos.isEmpty() ? "Unknown" : userInfos.get(0).getUserName();
        String employeeEmail = userRegisterClient.getUserEmail(ts.getUserId());
        UserDto approvalName = userRegisterClient.getUserNameByRole( "ACCOUNTS");
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee Email: " + employeeEmail);

        TimesheetResponse resp = new TimesheetResponse();
        resp.setTimesheetId(ts.getTimesheetId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeName(employeeName);
        resp.setEmployeeType(ts.getEmployeeType());
        resp.setTimesheetType(ts.getTimesheetType());
        resp.setTimesheetDate(ts.getTimesheetDate());
        resp.setWeekStartDate(ts.getWeekStartDate());
        resp.setWeekEndDate(ts.getWeekEndDate());

        try {
            resp.setWorkingEntries(mapper.readValue(
                    ts.getWorkingHours(),
                    new TypeReference<List<TimesheetEntry>>() {}
            ));
        } catch (Exception e) {
            System.err.println("❌ Failed to parse 'workingHours' JSON for Timesheet ID: " + ts.getTimesheetId());
            e.printStackTrace();
            resp.setWorkingEntries(List.of());
        }

        try {
            resp.setNonWorkingEntries(mapper.readValue(
                    ts.getNonWorkingHours(),
                    new TypeReference<List<TimesheetEntry>>() {}
            ));
        } catch (Exception e) {
            System.err.println("❌ Failed to parse 'nonWorkingHours' JSON for Timesheet ID: " + ts.getTimesheetId());
            e.printStackTrace();
            resp.setNonWorkingEntries(List.of());
        }

        resp.setPercentageOfTarget(ts.getPercentageOfTarget());
        resp.setStatus(ts.getStatus());
        resp.setApprover(approvalName == null ? "null" : approvalName.getUserName());
        resp.setApprovedBy(ts.getApprovedBy());
        resp.setApprovedAt(ts.getApprovedAt());
        resp.setNotes(ts.getNotes());

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

        if (employeeEmail != null && !employeeEmail.isBlank()) {
            try {
                System.out.println("Calling CandidateClient with email: " + employeeEmail);
                List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(employeeEmail);
                System.out.println("Number of placements received: " + (placements == null ? 0 : placements.size()));
                if (placements != null && !placements.isEmpty()) {
                    PlacementDetailsDto placement = placements.get(0); // pick first or apply logic
                    resp.setStartDate(placement.getStartDate());
                    resp.setClientName(placement.getClientName());
                    System.out.println("Placement startDate: " + placement.getStartDate() + ", clientName: " + placement.getClientName());
                } else {
                    System.out.println("No placements found for email: " + employeeEmail);
                    resp.setStartDate(null);
                    resp.setClientName(null);
                }
            } catch (Exception ex) {
                System.err.println("Error fetching placement details from candidate service for email: " + employeeEmail);
                ex.printStackTrace();
                resp.setStartDate(null);
                resp.setClientName(null);
            }
        } else {
            System.out.println("Employee email is null or blank, skipping candidate service call.");
            resp.setStartDate(null);
            resp.setClientName(null);
        }

        return resp;
    }

    @Transactional
    public Timesheet updateTimesheet(Long id, String userId, TimesheetRequest req) throws Exception {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        if (!ts.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to update this timesheet");
        }

        // Confirm type is WEEKLY to prevent misuse
        if (req.getType() != TimesheetType.WEEKLY) {
            throw new IllegalArgumentException("Only WEEKLY timesheet can be updated with this method");
        }

        ts.setTimesheetType(req.getType());
        ts.setTimesheetDate(req.getDate());

        // Update notes if present in request
        if (req.getNotes() != null) {
            ts.setNotes(req.getNotes());
        }

        // Serialize working and non-working entries separately
        ts.setWorkingHours(mapper.writeValueAsString(req.getWorkingEntries()));
        ts.setNonWorkingHours(mapper.writeValueAsString(req.getNonWorkingEntries()));

        // Calculate total working hours only
        double totalWorkingHours = calculateTotalHours(req.getWorkingEntries());
        ts.setPercentageOfTarget((totalWorkingHours / 40.0) * 100);

        return timesheetRepository.save(ts);
    }

    @Transactional
    public Timesheet updateTimesheetEntries(Long timesheetId, String userId,
                                            List<TimesheetEntry> updatedWorkingEntries, List<TimesheetEntry> updatedNonWorkingEntries) throws Exception {

        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        if (!ts.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to update this timesheet");
        }

        // Deserialize current entries
        List<TimesheetEntry> currentWorkingEntries;
        List<TimesheetEntry> currentNonWorkingEntries;
        try {
            currentWorkingEntries = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentWorkingEntries = new ArrayList<>();
        }
        try {
            currentNonWorkingEntries = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentNonWorkingEntries = new ArrayList<>();
        }

        // Update working entries
        mergeEntries(currentWorkingEntries, updatedWorkingEntries);

        // Update non-working entries (leave)
        mergeEntries(currentNonWorkingEntries, updatedNonWorkingEntries);

        ts.setWorkingHours(mapper.writeValueAsString(currentWorkingEntries));
        ts.setNonWorkingHours(mapper.writeValueAsString(currentNonWorkingEntries));

        // Calculate total working hours only
        double totalWorkingHours = currentWorkingEntries.stream().mapToDouble(TimesheetEntry::getHours).sum();
        double target = ts.getTimesheetType() == TimesheetType.DAILY ? 8.0 : 40.0;
        ts.setPercentageOfTarget((totalWorkingHours / target) * 100);

        return timesheetRepository.save(ts);
    }

    // Helper method to update existing entries by date or add new entries
    private void mergeEntries(List<TimesheetEntry> currentEntries, List<TimesheetEntry> updatedEntries) {
        for (TimesheetEntry updatedEntry : updatedEntries) {
            boolean matched = false;
            for (int i = 0; i < currentEntries.size(); i++) {
                TimesheetEntry existingEntry = currentEntries.get(i);
                if (existingEntry.getDate().equals(updatedEntry.getDate())) {
                    if (updatedEntry.getProject() != null) existingEntry.setProject(updatedEntry.getProject());
                    if (updatedEntry.getHours() != null) existingEntry.setHours(updatedEntry.getHours());
                    if (updatedEntry.getDescription() != null) existingEntry.setDescription(updatedEntry.getDescription());
                    currentEntries.set(i, existingEntry);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                currentEntries.add(updatedEntry);
            }
        }
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

