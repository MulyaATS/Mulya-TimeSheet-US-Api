package com.mulya.employee.timesheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.client.CandidateClient;
import com.mulya.employee.timesheet.client.UserRegisterClient;
import com.mulya.employee.timesheet.dto.*;
import com.mulya.employee.timesheet.exception.ResourceNotFoundException;
import com.mulya.employee.timesheet.exception.ValidationException;
import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.EmployeeLeaveSummary;
import com.mulya.employee.timesheet.model.Timesheet;
import com.mulya.employee.timesheet.model.TimesheetType;
import com.mulya.employee.timesheet.repository.AttachmentRepository;
import com.mulya.employee.timesheet.repository.EmployeeLeaveSummaryRepository;
import com.mulya.employee.timesheet.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
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

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private EmployeeLeaveSummaryRepository employeeLeaveSummaryRepository;

    private static final Logger logger = LoggerFactory.getLogger(TimesheetService.class);


    @Transactional
    public Timesheet createTimesheet(String userId, TimesheetRequest req) {
        Map<String, String> errors = new HashMap<>();
        UserDto user = userRegisterClient.getUserById(userId);
        if (user == null) {
            errors.put("userId", "User not found: " + userId);
            throw new ValidationException(errors);
        }

        String employeeWorkingType;
        try {
            employeeWorkingType = fetchEmployeeWorkingTypeFromPlacements(userId);
        } catch (Exception e) {
            employeeWorkingType = "WEEKLY";
            logger.warn("Could not fetch employee working type for userId {}: {}", userId, e.getMessage());
        }

        TimesheetType timesheetType = TimesheetType.valueOf(employeeWorkingType.toUpperCase());
        String employeeType = employeeWorkingType.equals("DAILY") ? "INTERNAL" : "EXTERNAL";

        if ("INTERNAL".equalsIgnoreCase(employeeType) && !employeeWorkingType.equalsIgnoreCase("DAILY")) {
            errors.put("timesheetType", "Internal employees must submit DAILY timesheets");
        }

        if ("EXTERNAL".equalsIgnoreCase(employeeType) && employeeWorkingType.equalsIgnoreCase("WEEKLY")) {
            LocalDate submitDate = req.getDate();
            if (submitDate.getDayOfWeek() != DayOfWeek.MONDAY) {
                LocalDate monthStart = submitDate.withDayOfMonth(1);
                if (!(submitDate.equals(monthStart) && submitDate.getDayOfWeek() != DayOfWeek.MONDAY)) {
                    errors.put("date", "Weekly timesheets must start on a Monday, or the actual month start for first partial week");
                }
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);

        String note = req.getNotes();
        LocalDate submitDate = req.getDate();
        LocalDate weekStart = submitDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(4);

        Timesheet ts = timesheetRepository.findByUserIdAndWeekStartDate(userId, weekStart)
                .orElseGet(() -> {
                    Timesheet t = new Timesheet();
                    t.setUserId(userId);
                    t.setTimesheetType(timesheetType);
                    t.setTimesheetDate(weekStart);
                    t.setWeekStartDate(weekStart);
                    t.setWeekEndDate(weekEnd);
                    t.setEmployeeType(employeeType);
                    t.setStatus("DRAFT");
                    t.setWorkingHours("[]");
                    t.setNonWorkingHours("[]");
                    t.setPercentageOfTarget(0.0);
                    t.setNotes(note);
                    t.setTimesheetId(generateNextTimesheetId());
                    logger.info("[Creation] New timesheet for week {} - {}", weekStart, weekEnd);
                    return t;
                });

        // Deserialize existing entries
        List<TimesheetEntry> currentWorkingHours;
        List<TimesheetEntry> currentNonWorkingHours;
        try {
            currentWorkingHours = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentWorkingHours = new ArrayList<>();
        }
        try {
            currentNonWorkingHours = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            currentNonWorkingHours = new ArrayList<>();
        }

        // Validate new entries are within week range
        for (TimesheetEntry newEntry : req.getWorkingEntries()) {
            if (newEntry.getDate().isBefore(weekStart) || newEntry.getDate().isAfter(weekEnd)) {
                throw new IllegalArgumentException("Entry date " + newEntry.getDate() + " is outside the current timesheet's week range.");
            }
        }
        for (TimesheetEntry newEntry : req.getNonWorkingEntries()) {
            if (newEntry.getDate().isBefore(weekStart) || newEntry.getDate().isAfter(weekEnd)) {
                throw new IllegalArgumentException("Entry date " + newEntry.getDate() + " is outside the current timesheet's week range.");
            }
        }

        // REMOVE old leave entries (nonWorkingHours) for dates where a workingHours entry is being added
        Set<LocalDate> newWorkingDates = req.getWorkingEntries().stream()
                .map(TimesheetEntry::getDate)
                .collect(Collectors.toSet());

        List<TimesheetEntry> cancelledLeaves = currentNonWorkingHours.stream()
                .filter(e -> newWorkingDates.contains(e.getDate()) && e.getHours() == 8.0)
                .collect(Collectors.toList());

        currentNonWorkingHours.removeIf(entry -> newWorkingDates.contains(entry.getDate()));

        // Remove replaced working entries for same date, merge new
        currentWorkingHours.removeIf(entry -> newWorkingDates.contains(entry.getDate()));
        currentWorkingHours.addAll(req.getWorkingEntries());

        // Remove replaced non-working entries for same date, merge new
        Set<LocalDate> newNonWorkingDates = req.getNonWorkingEntries().stream()
                .map(TimesheetEntry::getDate)
                .collect(Collectors.toSet());
        currentNonWorkingHours.removeIf(entry -> newNonWorkingDates.contains(entry.getDate()));
        currentNonWorkingHours.addAll(req.getNonWorkingEntries());

        // Serialize back
        try {
            ts.setWorkingHours(mapper.writeValueAsString(currentWorkingHours));
            ts.setNonWorkingHours(mapper.writeValueAsString(currentNonWorkingHours));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing working/non-working hours JSON", e);
        }

        // Calculate workdays count for the whole week
        int workingDaysCount = 0;
        LocalDate cursor = weekStart;
        while (!cursor.isAfter(weekEnd)) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDaysCount++;
            }
            cursor = cursor.plusDays(1);
        }
        double targetHours = workingDaysCount * 8.0;

        // Get employee full-time status for leave inclusion
        String employeeEmail = userRegisterClient.getUserEmail(userId);
        String fullEmployeeType = "Unknown";
        if (employeeEmail != null && !employeeEmail.isBlank()) {
            try {
                List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(employeeEmail);
                if (placements != null && !placements.isEmpty()) {
                    fullEmployeeType = placements.get(0).getEmployeeType();
                }
            } catch (Exception ex) {
                logger.warn("Could not fetch placement data for email {}: {}", employeeEmail, ex.getMessage());
            }
        }
        boolean isFullTime = "Full-time".equalsIgnoreCase(fullEmployeeType);

        double totalWorkingHours = currentWorkingHours.stream().mapToDouble(TimesheetEntry::getHours).sum();
        double totalNonWorkingHours = currentNonWorkingHours.stream().mapToDouble(TimesheetEntry::getHours).sum();

        double effectiveWorkingHours = totalWorkingHours + (isFullTime ? totalNonWorkingHours : 0.0);

        double percentageOfTarget = targetHours == 0 ? 0 : (effectiveWorkingHours / targetHours) * 100;
        ts.setPercentageOfTarget(percentageOfTarget);

        // If any leave is cancelled (for full-time), refund that leave in leaveSummary
        if (isFullTime && !cancelledLeaves.isEmpty()) {
            EmployeeLeaveSummary leaveSummary = employeeLeaveSummaryRepository.findByUserId(userId).orElse(null);
            if (leaveSummary != null) {
                int refundCount = cancelledLeaves.size();
                leaveSummary.setAvailableLeaves(leaveSummary.getAvailableLeaves() + refundCount);
                leaveSummary.setTakenLeaves(Math.max(0, leaveSummary.getTakenLeaves() - refundCount));
                employeeLeaveSummaryRepository.save(leaveSummary);
                logger.info("[Leave Refund] Refunded {} leaves for userId {}", refundCount, userId);
            }
        }

        return timesheetRepository.save(ts);
    }



    private String fetchEmployeeWorkingTypeFromPlacements(String userId) throws Exception {
        // 1. Get employee email by userID
        String email = userRegisterClient.getUserEmail(userId);
        if (email == null || email.isBlank()) {
            throw new Exception("User email not found for userId: " + userId);
        }

        // 2. Fetch placements by email
        List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(email);
        if (placements == null || placements.isEmpty()) {
            throw new Exception("No placements found for email: " + email);
        }

        // 3. Extract employee working type from first placement (adjust logic if multiple)
        String employeeWorkingType = placements.get(0).getEmployeeWorkingType();
        if (employeeWorkingType == null || employeeWorkingType.isBlank()) {
            throw new Exception("Employee working type not set in placement for email: " + email);
        }

        return employeeWorkingType.toUpperCase(); // Ensure uppercase for enum consistency
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
    public List<Timesheet> submitMonthly(String userId, LocalDate monthStartDate) {
        if (monthStartDate.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Month start date must be the first day of the month");
        }
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());

        List<Timesheet> timesheets = timesheetRepository.findTimesheetsOverlappingMonth(userId, monthStartDate, monthEndDate);

        if (timesheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found overlapping the month " + monthStartDate);
        }

        UserDto managerDto = userRegisterClient.getUsersByRole("ACCOUNTS")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ACCOUNTS manager"));
        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerDto.getUserId()).get(0);

        for (Timesheet ts : timesheets) {
            ts.setStatus("PENDING_APPROVAL");
            UserInfoDto empInfo = userRegisterClient.getUserInfos(ts.getUserId()).get(0);

            emailService.sendManagerApprovalRequestEmail(
                    ts,
                    managerDto.getEmail(),
                    managerInfo.getUserName(),
                    empInfo.getUserName()
            );
        }

        return timesheetRepository.saveAll(timesheets);
    }


    @Transactional
    public Timesheet approveTimesheet(String id, String managerUserId) {
        Timesheet ts = timesheetRepository.findByTimesheetId(id)
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
    public Timesheet rejectTimesheet(String timesheetId, String managerUserId, String reason) {
        Timesheet ts = timesheetRepository.findByTimesheetId(timesheetId)
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

    @Transactional
    public List<Timesheet> approveMonthlyTimesheets(String userId, LocalDate monthStart, LocalDate monthEnd, String managerUserId) {
        List<Timesheet> timesheets = timesheetRepository.findTimesheetsOverlappingMonth(userId, monthStart, monthEnd);

        if (timesheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found overlapping the month");
        }

        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerUserId).get(0);
        if (managerInfo.getUserName() == null) {
            throw new IllegalStateException("Manager name not found for " + managerUserId);
        }

        for (Timesheet ts : timesheets) {
            ts.setStatus("APPROVED");
            ts.setApprovedAt(LocalDateTime.now());
            ts.setApprovedBy(managerInfo.getUserName());
        }

        Timesheet savedTimesheets = timesheetRepository.saveAll(timesheets).get(0);
        // Just fetch one to get user info - they are all for same user

        // Send single consolidated monthly approval email
        UserInfoDto empInfo = userRegisterClient.getUserInfos(userId).get(0);
        String empEmail = userRegisterClient.getUserEmail(userId);
        String monthStartStr = monthStart.toString();
        String monthEndStr = monthEnd.toString();

        emailService.sendEmployeeMonthlyApprovalEmail(
                empEmail,
                empInfo.getUserName(),
                monthStartStr,
                monthEndStr
        );

        return timesheets;
    }


    @Transactional
    public List<Timesheet> rejectMonthlyTimesheets(String userId, LocalDate monthStart, LocalDate monthEnd, String managerUserId, String reason) {
        List<Timesheet> timesheets = timesheetRepository.findTimesheetsOverlappingMonth(userId, monthStart, monthEnd);

        if (timesheets.isEmpty()) {
            throw new IllegalArgumentException("No timesheets found overlapping the month");
        }

        UserInfoDto managerInfo = userRegisterClient.getUserInfos(managerUserId).get(0);
        if (managerInfo.getUserName() == null) {
            throw new IllegalStateException("Manager name not found for " + managerUserId);
        }

        for (Timesheet ts : timesheets) {
            ts.setStatus("REJECTED");
            ts.setApprovedAt(LocalDateTime.now());
            ts.setApprovedBy(managerInfo.getUserName());
        }

        timesheetRepository.saveAll(timesheets);

        // Send a single consolidated rejection email
        UserInfoDto empInfo = userRegisterClient.getUserInfos(userId).get(0);
        String empEmail = userRegisterClient.getUserEmail(userId);
        String monthStartStr = monthStart.toString();
        String monthEndStr = monthEnd.toString();

        emailService.sendEmployeeMonthlyRejectionEmail(
                empEmail,
                empInfo.getUserName(),
                monthStartStr,
                monthEndStr,
                reason
        );

        return timesheets;
    }


    public Page<TimesheetApprovalDto> getTimesheetsByStatus(String status, String managerUserId, Pageable pageable) {
        Page<Timesheet> timesheetPage = timesheetRepository.findByStatus(status, pageable);
        return timesheetPage.map(ts -> toApprovalDto(ts, managerUserId));
    }

    public String getDefaultManagerUserId() {
        // Example: return the first user with "ACCOUNTS" role
        return userRegisterClient.getUsersByRole("ACCOUNTS")
                .stream()
                .findFirst()
                .map(UserDto::getUserId)
                .orElse(null);
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
        dto.setStatus(ts.getStatus());

        return dto;
    }


    private double calculateTotalHours(List<TimesheetEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Entries cannot be empty");
        }
        return entries.stream().mapToDouble(TimesheetEntry::getHours).sum();
    }

    public MonthlyTimesheetResponse getTimesheetsByUserIdAndMonth(String userId, LocalDate monthStart, LocalDate monthEnd) {
        List<Timesheet> timesheets = timesheetRepository.findTimesheetsOverlappingMonth(userId, monthStart, monthEnd);

        final double[] totalMonthlyWorkingHours = {0.0};
        List<TimesheetResponse> dtos = timesheets.stream()
                .map(ts -> mapToResponse(ts, monthStart, monthEnd))
                .map(resp -> {
                    // Filter working entries by month
                    resp.setWorkingEntries(resp.getWorkingEntries().stream()
                            .filter(e -> {
                                LocalDate d = LocalDate.parse(e.getDate().toString());
                                return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                            }).collect(Collectors.toList()));

                    // Filter non-working entries by month
                    resp.setNonWorkingEntries(resp.getNonWorkingEntries().stream()
                            .filter(e -> {
                                LocalDate d = LocalDate.parse(e.getDate().toString());
                                return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                            }).collect(Collectors.toList()));

                    try {
                        double sumThisSheet = resp.getWorkingEntries().stream()
                                .mapToDouble(TimesheetEntry::getHours)
                                .sum();
                        totalMonthlyWorkingHours[0] += sumThisSheet;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return resp;
                }).collect(Collectors.toList());

        MonthlyTimesheetResponse response = new MonthlyTimesheetResponse();
        response.setTimesheets(dtos);
        response.setTotalWorkingHours(totalMonthlyWorkingHours[0]);
        response.setMonthStartDate(monthStart);
        response.setMonthEndDate(monthEnd);
        return response;
    }



    public List<TimesheetResponse> getAllTimesheetsByUserId(String userId) {
        List<Timesheet> timesheets = timesheetRepository.findByUserId(userId);
        return timesheets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public double calculateProportionalTargetForMonth(
            Timesheet ts,
            LocalDate monthStart,
            LocalDate monthEnd,
            boolean isFullTime) {

        LocalDate weekStart = ts.getWeekStartDate();
        LocalDate weekEnd = ts.getWeekEndDate();

        // Calculate overlap between timesheet week and requested month
        LocalDate overlapStart = weekStart.isBefore(monthStart) ? monthStart : weekStart;
        LocalDate overlapEnd = weekEnd.isAfter(monthEnd) ? monthEnd : weekEnd;

        if (overlapEnd.isBefore(overlapStart)) {
            return 0.0;
        }

        // Count workdays (Mon-Fri) in overlap
        int workdays = 0;
        for (LocalDate d = overlapStart; !d.isAfter(overlapEnd); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                workdays++;
            }
        }

        double targetHours = workdays * 8.0;

        List<TimesheetEntry> workingEntries;
        List<TimesheetEntry> nonWorkingEntries;
        try {
            workingEntries = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
            nonWorkingEntries = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }

        // Filter entries in the overlap period
        List<TimesheetEntry> filteredWorking = workingEntries.stream()
                .filter(e -> !e.getDate().isBefore(overlapStart) && !e.getDate().isAfter(overlapEnd))
                .collect(Collectors.toList());

        List<TimesheetEntry> filteredNonWorking = nonWorkingEntries.stream()
                .filter(e -> !e.getDate().isBefore(overlapStart) && !e.getDate().isAfter(overlapEnd))
                .collect(Collectors.toList());

        double totalWorkingHours = filteredWorking.stream().mapToDouble(TimesheetEntry::getHours).sum();
        double totalLeaveHours = filteredNonWorking.stream().mapToDouble(TimesheetEntry::getHours).sum();

        double effectiveHours = totalWorkingHours + (isFullTime ? totalLeaveHours : 0);

        if (targetHours == 0) return 0.0;

        return (effectiveHours / targetHours) * 100.0;
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
        UserDto approvalName = userRegisterClient.getUserNameByRole("ACCOUNTS");
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee Email: " + employeeEmail);

        TimesheetResponse resp = new TimesheetResponse();
        resp.setTimesheetId(ts.getTimesheetId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeName(employeeName);
        resp.setEmployeeType(ts.getEmployeeType());

        // Fetch timesheetType dynamically from placements:
        if (employeeEmail != null && !employeeEmail.isBlank()) {
            try {
                System.out.println("Calling CandidateClient with email: " + employeeEmail);
                List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(employeeEmail);
                System.out.println("Number of placements received: " + (placements == null ? 0 : placements.size()));
                if (placements != null && !placements.isEmpty()) {
                    PlacementDetailsDto placement = placements.get(0);
                    resp.setTimesheetType(TimesheetType.valueOf(placement.getEmployeeWorkingType()));
                    resp.setStartDate(placement.getStartDate());
                    resp.setClientName(placement.getClientName());
                    resp.setEmployeeRoleType(placement.getEmployeeType());
                    System.out.println("Timesheet employeeType (from entity): " + placement.getEmployeeType());
                } else {
                    resp.setTimesheetType(TimesheetType.WEEKLY); // fallback default
                }
            } catch (Exception ex) {
                System.err.println("Error fetching placement details for email: " + employeeEmail);
                ex.printStackTrace();
                resp.setTimesheetType(TimesheetType.WEEKLY);
            }
        } else {
            resp.setTimesheetType(TimesheetType.WEEKLY);
        }

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

        return resp;
    }

    private TimesheetResponse mapToResponse(Timesheet ts, LocalDate monthStart, LocalDate monthEnd) {
        System.out.println("Mapping Timesheet ID: " + ts.getTimesheetId() + ", User ID: " + ts.getUserId());

        List<UserInfoDto> userInfos = userRegisterClient.getUserInfos(ts.getUserId());
        String employeeName = userInfos.isEmpty() ? "Unknown" : userInfos.get(0).getUserName();
        String employeeEmail = userRegisterClient.getUserEmail(ts.getUserId());
        UserDto approvalName = userRegisterClient.getUserNameByRole("ACCOUNTS");
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee Email: " + employeeEmail);

        TimesheetResponse resp = new TimesheetResponse();
        resp.setTimesheetId(ts.getTimesheetId());
        resp.setUserId(ts.getUserId());
        resp.setEmployeeName(employeeName);
        resp.setEmployeeType(ts.getEmployeeType());

        boolean isFullTime = false;
        // Fetch timesheetType dynamically from placements and determine isFullTime
        if (employeeEmail != null && !employeeEmail.isBlank()) {
            try {
                System.out.println("Calling CandidateClient with email: " + employeeEmail);
                List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(employeeEmail);
                System.out.println("Number of placements received: " + (placements == null ? 0 : placements.size()));
                if (placements != null && !placements.isEmpty()) {
                    PlacementDetailsDto placement = placements.get(0);
                    resp.setTimesheetType(TimesheetType.valueOf(placement.getEmployeeWorkingType()));
                    resp.setStartDate(placement.getStartDate());
                    resp.setClientName(placement.getClientName());
                    resp.setEmployeeRoleType(placement.getEmployeeType());
                    System.out.println("Timesheet employeeType (from entity): " + placement.getEmployeeType());

                    isFullTime = "Full-time".equalsIgnoreCase(placement.getEmployeeType());
                } else {
                    resp.setTimesheetType(TimesheetType.WEEKLY); // fallback default
                }
            } catch (Exception ex) {
                System.err.println("Error fetching placement details for email: " + employeeEmail);
                ex.printStackTrace();
                resp.setTimesheetType(TimesheetType.WEEKLY);
            }
        } else {
            resp.setTimesheetType(TimesheetType.WEEKLY);
        }

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

        // Now calculate proportional target percentage adjusted for leaves with isFullTime flag
        double proportionalTarget = calculateTargetPercentageAdjustedForLeaves(ts, monthStart, monthEnd, isFullTime);
        resp.setPercentageOfTarget(proportionalTarget);

        resp.setStatus(ts.getStatus());
        resp.setApprover(approvalName == null ? "null" : approvalName.getUserName());
        resp.setApprovedBy(ts.getApprovedBy());
        resp.setApprovedAt(ts.getApprovedAt());
        resp.setNotes(ts.getNotes());

        if (ts.getAttachments() != null) {
            List<AttachmentDto> filteredAttachments = ts.getAttachments()
                    .stream()
                    .filter(att -> !att.getAttachmentEndDate().isBefore(monthStart) &&
                            !att.getAttachmentStartDate().isAfter(monthEnd))
                    .map(att -> new AttachmentDto(
                            att.getId(),
                            att.getFilename(),
                            att.getFiletype(),
                            att.getUploadedAt()
                    ))
                    .collect(Collectors.toList());
            resp.setAttachments(filteredAttachments);
        } else {
            resp.setAttachments(List.of());
        }
        return resp;
    }

    // Overloaded calculateTargetPercentageAdjustedForLeaves which accepts isFullTime flag
    private double calculateTargetPercentageAdjustedForLeaves(
            Timesheet ts,
            LocalDate monthStart,
            LocalDate monthEnd,
            boolean isFullTime) {

        LocalDate weekStart = ts.getWeekStartDate();
        LocalDate weekEnd = ts.getWeekEndDate();

        long totalDays = ChronoUnit.DAYS.between(weekStart, weekEnd) + 1;

        LocalDate overlapStart = weekStart.isBefore(monthStart) ? monthStart : weekStart;
        LocalDate overlapEnd = weekEnd.isAfter(monthEnd) ? monthEnd : weekEnd;
        long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;

        if (overlapDays <= 0) {
            return 0.0;
        }

        List<TimesheetEntry> workingEntries = List.of();
        List<TimesheetEntry> nonWorkingEntries = List.of();

        try {
            workingEntries = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
            nonWorkingEntries = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
        } catch (Exception e) {
            System.err.println("Failed to parse entries for Timesheet ID: " + ts.getTimesheetId());
            e.printStackTrace();
        }

        // Count leave days within overlapping period using nonWorking entries with hours == 8
        long leaveDays = nonWorkingEntries.stream()
                .filter(e -> {
                    LocalDate entryDate = e.getDate();
                    return !entryDate.isBefore(overlapStart)
                            && !entryDate.isAfter(overlapEnd)
                            && e.getHours() == 8.0;
                })
                .count();

        long workingDays = overlapDays - leaveDays;
        if (workingDays < 0) workingDays = 0;

        // Sum working hours in overlapping period
        double totalWorkingHours = workingEntries.stream()
                .filter(e -> {
                    LocalDate d = e.getDate();
                    return !d.isBefore(overlapStart) && !d.isAfter(overlapEnd);
                })
                .mapToDouble(TimesheetEntry::getHours)
                .sum();

        // Sum leave hours if full time
        double totalLeaveHours = 0;
        if (isFullTime) {
            totalLeaveHours = leaveDays * 8.0;
        }

        double targetHours = overlapDays * 8.0;
        if (targetHours == 0) {
            return 0.0;
        }

        double effectiveHours = totalWorkingHours + totalLeaveHours;
        double percentage = (effectiveHours / targetHours) * 100.0;

        return percentage;
    }

    @Transactional
    public Timesheet updateTimesheet(String timesheetId, String userId, TimesheetRequest req) throws Exception {
        Timesheet ts = timesheetRepository.findByTimesheetId(timesheetId)
                .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));

        if (!ts.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to update this timesheet");
        }

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
    public Timesheet updateTimesheetEntries(String timesheetId, String userId,
                                            List<TimesheetEntry> updatedWorkingEntries, List<TimesheetEntry> updatedNonWorkingEntries) throws Exception {

        Timesheet ts = timesheetRepository.findByTimesheetId(timesheetId)
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

        // === Updated target percentage calculation here ===
        double totalWorkingHours = currentWorkingEntries.stream().mapToDouble(TimesheetEntry::getHours).sum();

        double target;

        if (ts.getTimesheetType() == TimesheetType.DAILY) {
            target = 8.0;
        } else {
            // For weekly timesheets, calculate target as full working hours of full week (Mon-Fri)
            LocalDate weekStart = ts.getWeekStartDate();
            LocalDate weekEnd = ts.getWeekEndDate();

            long workingDaysCount = 0;
            LocalDate d = weekStart;
            while (!d.isAfter(weekEnd)) {
                DayOfWeek day = d.getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    workingDaysCount++;
                }
                d = d.plusDays(1);
            }
            target = workingDaysCount * 8.0;
        }

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
    public Timesheet uploadAttachments(String timesheetId, List<MultipartFile> files,
                                       LocalDate attachmentStartDate, LocalDate attachmentEndDate) throws IOException {
        Timesheet ts = timesheetRepository.findByTimesheetId(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with ID: " + timesheetId, ResourceNotFoundException.ResourceType.TIMESHEET));

        // Validate attachment dates within timesheet week
        if (attachmentStartDate.isBefore(ts.getWeekStartDate()) || attachmentEndDate.isAfter(ts.getWeekEndDate())) {
            throw new IllegalArgumentException("Attachment date range must be within the timesheet week");
        }

        // Optional: Check for overlapping attachments on same date range
        boolean exists = ts.getAttachments().stream().anyMatch(a ->
                !(a.getAttachmentEndDate().isBefore(attachmentStartDate) || a.getAttachmentStartDate().isAfter(attachmentEndDate))
        );

        if (exists) {
            throw new IllegalStateException(String.format("Attachment already exists for dates %s to %s",
                    attachmentStartDate, attachmentEndDate));
        }

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Construct new filename in desired format
            String newFilename = String.format("timesheet_%s_to_%s%s",
                    attachmentStartDate.toString(),
                    attachmentEndDate.toString(),
                    extension);

            Attachment attachment = new Attachment();
            attachment.setTimesheet(ts);
            attachment.setFilename(newFilename);
            attachment.setFiletype(file.getContentType());
            attachment.setContent(file.getBytes());
            attachment.setWeekStartDate(ts.getWeekStartDate());
            attachment.setWeekEndDate(ts.getWeekEndDate());
            attachment.setAttachmentStartDate(attachmentStartDate);
            attachment.setAttachmentEndDate(attachmentEndDate);
            ts.getAttachments().add(attachment);
        }

        return timesheetRepository.save(ts);
    }


    public List<EmployeeMonthlyTimesheetDto> getAllEmployeesMonthlySummary(LocalDate monthStart, LocalDate monthEnd) throws Exception {
        logger.info("Fetching timesheets from {} to {}", monthStart, monthEnd);

        // Extend query period to earliest Monday before monthStart for partial weeks
        LocalDate firstMonday = monthStart;
        while (firstMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            firstMonday = firstMonday.minusDays(1);
        }

        List<Timesheet> timesheets = timesheetRepository.findByWeekStartDateBetween(firstMonday, monthEnd);

        Map<String, List<Timesheet>> byUser = timesheets.stream()
                .collect(Collectors.groupingBy(Timesheet::getUserId));

        List<Week> calendarWeeks = getWeeksMondayToFridayForMonth(monthStart, monthEnd);

        Map<String, Integer> statusPriority = Map.of(
                "DRAFT", 1,
                "PENDING_APPROVAL", 2,
                "REJECTED", 3,
                "APPROVED", 4
        );

        Set<String> userIds = byUser.keySet();

        // Bulk fetch leave summaries for all users
        Map<String, EmployeeLeaveSummary> leaveSummariesMap = employeeLeaveSummaryRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(EmployeeLeaveSummary::getUserId, ls -> ls));

        List<EmployeeMonthlyTimesheetDto> summaries = new ArrayList<>();

        for (Map.Entry<String, List<Timesheet>> entry : byUser.entrySet()) {
            String userId = entry.getKey();
            List<Timesheet> empTimesheets = entry.getValue();

            double[] weeklyWorkHours = new double[calendarWeeks.size()];
            double[] weeklyLeaveHours = new double[calendarWeeks.size()];
            String[] weeklyStatuses = new String[calendarWeeks.size()];
            Arrays.fill(weeklyStatuses, "NO_TIMESHEET");

            for (Timesheet ts : empTimesheets) {
                LocalDate tsDate = ts.getWeekStartDate();

                int weekIndex = -1;
                for (int i = 0; i < calendarWeeks.size(); i++) {
                    Week w = calendarWeeks.get(i);
                    if (!tsDate.isBefore(w.weekStart) && !tsDate.isAfter(w.weekEnd)) {
                        weekIndex = i;
                        break;
                    }
                }

                if (weekIndex == -1) {
                    logger.warn("Timesheet weekStartDate {} not in any week", tsDate);
                    continue;
                }

                Week currentWeek = calendarWeeks.get(weekIndex);

                List<TimesheetEntry> workingEntries = mapper.readValue(ts.getWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});
                List<TimesheetEntry> nonWorkingEntries = mapper.readValue(ts.getNonWorkingHours(), new TypeReference<List<TimesheetEntry>>() {});

                double workHours = workingEntries.stream()
                        .filter(e -> currentWeek.daysInsideMonth.contains(LocalDate.parse(e.getDate().toString())))
                        .mapToDouble(TimesheetEntry::getHours)
                        .sum();

                double leaveHours = nonWorkingEntries.stream()
                        .filter(e -> currentWeek.daysInsideMonth.contains(LocalDate.parse(e.getDate().toString())))
                        .mapToDouble(TimesheetEntry::getHours)
                        .sum();

                weeklyWorkHours[weekIndex] += workHours;
                weeklyLeaveHours[weekIndex] += leaveHours;

                String tsStatus = ts.getStatus() != null ? ts.getStatus() : "NO_TIMESHEET";
                if (weeklyStatuses[weekIndex].equals("NO_TIMESHEET") ||
                        statusPriority.getOrDefault(tsStatus, Integer.MAX_VALUE) < statusPriority.getOrDefault(weeklyStatuses[weekIndex], Integer.MAX_VALUE)) {
                    weeklyStatuses[weekIndex] = tsStatus;
                }
            }

            String aggregatedStatus = Arrays.stream(weeklyStatuses)
                    .filter(statusPriority::containsKey)
                    .min(Comparator.comparingInt(statusPriority::get))
                    .orElse("NO_TIMESHEET");

            List<UserInfoDto> userInfoList = userRegisterClient.getUserInfos(userId);
            String employeeName = userInfoList.isEmpty() ? "Unknown" : userInfoList.get(0).getUserName();
            String employeeEmail = userRegisterClient.getUserEmail(userId);

            String employeeType = "Unknown";
            LocalDate joiningDate = null;
            String clientName = null;
            try {
                if (employeeEmail != null && !employeeEmail.isBlank()) {
                    List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(employeeEmail);
                    if (placements != null && !placements.isEmpty()) {
                        PlacementDetailsDto placement = placements.get(0);
                        employeeType = placement.getEmployeeType();
                        joiningDate = placement.getStartDate();
                        clientName = placement.getClientName();
                    }
                }
            } catch (ResourceNotFoundException ex) {
                logger.warn("No placement details found for email {}: {}", employeeEmail, ex.getMessage());
            }

            double totalWorkingHours = Arrays.stream(weeklyWorkHours).sum();
            double totalLeaveHours = Arrays.stream(weeklyLeaveHours).sum();

            // Use leave summary from DB - no saving, no recalculation
            EmployeeLeaveSummary leaveSummary = leaveSummariesMap.get(userId);

            int availableLeaves = leaveSummary != null ? leaveSummary.getAvailableLeaves() : 0;
            int takenLeaves = leaveSummary != null ? leaveSummary.getTakenLeaves() : 0;

            // Adjust working hours for full-time paid leave hours included
            if (availableLeaves >= 0 && "Full-time".equalsIgnoreCase(employeeType)) {
                totalWorkingHours += totalLeaveHours;
                for (int i = 0; i < weeklyWorkHours.length; i++) {
                    weeklyWorkHours[i] += weeklyLeaveHours[i];
                }
            }

            EmployeeMonthlyTimesheetDto dto = new EmployeeMonthlyTimesheetDto();
            dto.setEmployeeId(userId);
            dto.setEmployeeName(employeeName);
            dto.setEmployeeType(employeeType);
            dto.setClientName(clientName);
            dto.setMonthStartDate(monthStart);
            dto.setMonthEndDate(monthEnd);
            dto.setJoiningDate(joiningDate);
            dto.setStatus(aggregatedStatus);

            dto.setWeek1Hours(calendarWeeks.size() > 0 ? (int) Math.round(weeklyWorkHours[0]) : 0);
            dto.setWeek2Hours(calendarWeeks.size() > 1 ? (int) Math.round(weeklyWorkHours[1]) : 0);
            dto.setWeek3Hours(calendarWeeks.size() > 2 ? (int) Math.round(weeklyWorkHours[2]) : 0);
            dto.setWeek4Hours(calendarWeeks.size() > 3 ? (int) Math.round(weeklyWorkHours[3]) : 0);
            dto.setWeek5Hours(calendarWeeks.size() > 4 ? (int) Math.round(weeklyWorkHours[4]) : 0);

            dto.setTotalWorkingHours((int) Math.round(totalWorkingHours));
            dto.setTotalWorkingDays((int) Math.round(totalWorkingHours / 8.0));

            dto.setAvailableLeaves(availableLeaves);
            dto.setTakenLeaves(takenLeaves);

            summaries.add(dto);
        }

        logger.info("Completed processing monthly summaries for {} employees", summaries.size());
        return summaries;
    }

    private List<Week> getWeeksMondayToFridayForMonth(LocalDate monthStart, LocalDate monthEnd) {
        List<Week> weeks = new ArrayList<>();
        LocalDate firstMonday = monthStart;
        while (firstMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            firstMonday = firstMonday.minusDays(1);
        }
        LocalDate currentStart = firstMonday;
        while (!currentStart.isAfter(monthEnd)) {
            LocalDate currentEnd = currentStart.plusDays(4); // Monday to Friday
            List<LocalDate> daysInsideMonth = new ArrayList<>();
            for (LocalDate d = currentStart; !d.isAfter(currentEnd); d = d.plusDays(1)) {
                if (!d.isBefore(monthStart) && !d.isAfter(monthEnd)) {
                    daysInsideMonth.add(d);
                }
            }
            weeks.add(new Week(currentStart, currentEnd, daysInsideMonth));
            currentStart = currentStart.plusWeeks(1);
        }
        return weeks;
    }

    public List<String> getVendorNamesByUserId(String userId) {
        String userEmail = userRegisterClient.getUserEmail(userId); // may throw ResourceNotFoundException

        List<PlacementDetailsDto> placements = candidateClient.getPlacementsByEmail(userEmail); // may also throw ResourceNotFoundException

        return placements.stream()
                .map(PlacementDetailsDto::getClientName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

}

