package com.mulya.employee.timesheet.service;

import com.mulya.employee.timesheet.dto.EmployeeLeaveSummaryDto;
import com.mulya.employee.timesheet.model.EmployeeLeaveSummary;
import com.mulya.employee.timesheet.model.LeaveCarryForward;
import com.mulya.employee.timesheet.repository.EmployeeLeaveSummaryRepository;
import com.mulya.employee.timesheet.repository.LeaveCarryForwardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class LeaveService {
    @Autowired
    private EmployeeLeaveSummaryRepository employeeLeaveSummaryRepository;

    @Autowired
    private LeaveCarryForwardRepository repository;

    /**
     * Enhanced method: Calculates carry forward leaves AND saves to database
     */
    @Transactional
    public LeaveCalculationResult calculateAndSaveCurrentLeaves(String userId, String employeeName,
                                                                String employeeType, LocalDate joiningDate,
                                                                LocalDate monthStart, LocalDate monthEnd,
                                                                double totalLeaveHours, String updatedBy) {

        LeaveCalculationResult result = new LeaveCalculationResult();
        double totalLeavesSpent = totalLeaveHours / 8.0;

        if ("Full-time".equalsIgnoreCase(employeeType)) {
            // **REPLICATE ORIGINAL LOGIC EXACTLY**

            // Calculate carry forward leaves based on only FULL months before this month
            int calculatedCarryForwardLeaves = calculateCurrentCarryForwardLeaves(userId, joiningDate, monthStart);

            // Calculate monthly increment: Only 1 leave for current month if joined before monthEnd
            int monthlyIncrementedLeaves = 0;
            if (joiningDate != null && !joiningDate.isAfter(monthEnd)) {
                monthlyIncrementedLeaves = 1;
            }

            // Use calculated carry forward leaves (ensure it's not double counting)
            int totalLeavesAvailable = calculatedCarryForwardLeaves + monthlyIncrementedLeaves;

            result.availableLeaves = totalLeavesAvailable;
            result.takenLeaves = (int) Math.round(totalLeavesSpent);
            result.leaveBalance = totalLeavesAvailable - result.takenLeaves;

            // Handle unpaid leaves (same as original logic)
            if (totalLeavesSpent <= totalLeavesAvailable) {
                // No adjustment needed - leaves are within available
                result.unpaidLeaveDays = 0;
            } else {
                // Deduct unpaid leave hours for extra leaves taken beyond available
                int unpaidLeaveDays = (int) (totalLeavesSpent - totalLeavesAvailable);
                result.unpaidLeaveDays = unpaidLeaveDays;
                // Keep actual taken leaves as calculated, don't adjust
            }

            // Save carry forward leaves (remaining balance) for next month
            saveCarryForwardLeaves(userId, Math.max(0, totalLeavesAvailable - result.takenLeaves));

        } else {
            // **FOR NON-FULL TIME EMPLOYEES - MATCH ORIGINAL LOGIC**
            result.availableLeaves = 0;
            result.takenLeaves = (int) Math.round(totalLeavesSpent);
            result.leaveBalance = result.availableLeaves - result.takenLeaves; // This will be negative
            result.unpaidLeaveDays = result.takenLeaves; // All leaves are unpaid
        }

        // **SAVE TO DATABASE**
        saveOrUpdateLeaveSummary(userId, employeeName, result.availableLeaves,
                result.takenLeaves, result.leaveBalance, updatedBy);

        return result;
    }

    // **Helper Result Class**
    public static class LeaveCalculationResult {
        public int availableLeaves;
        public int takenLeaves;
        public int leaveBalance;
        public int unpaidLeaveDays;

        // Optional: Add getters if needed
        public int getAvailableLeaves() { return availableLeaves; }
        public int getTakenLeaves() { return takenLeaves; }
        public int getLeaveBalance() { return leaveBalance; }
        public int getUnpaidLeaveDays() { return unpaidLeaveDays; }
    }


    @Transactional
    public void saveCarryForwardLeaves(String userId, int carryForward) {
        LeaveCarryForward entity = new LeaveCarryForward(userId, carryForward);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public int calculateCurrentCarryForwardLeaves(String userId, LocalDate joiningDate, LocalDate monthStart) {
        if (joiningDate == null || joiningDate.isAfter(monthStart)) {
            return 0; // no leaves if joined after this month
        }
        // Calculate month before current month
        LocalDate monthBefore = monthStart.minusMonths(1).withDayOfMonth(1);

        // Total full months elapsed from joining to previous month start
        long monthsElapsed = ChronoUnit.MONTHS.between(joiningDate.withDayOfMonth(1), monthBefore);

        int totalLeavesAccrued = (int) Math.max(monthsElapsed, 0);

        // Fetch leaves used till end of last month (you need to implement this)
        int leavesUsedTillLastMonth = fetchLeavesUsedTillDate(userId, joiningDate, monthBefore.withDayOfMonth(monthBefore.lengthOfMonth()));

        // Carry forward leaves = accrued - used (no negative allowed)
        int currentCarryForward = totalLeavesAccrued - leavesUsedTillLastMonth;

        return Math.max(currentCarryForward, 0);
    }

    private int fetchLeavesUsedTillDate(String userId, LocalDate joiningDate, LocalDate tillDate) {
        // Implementation depends on your leave records system
        return 0;
    }

    // Save or update leave summary
    public EmployeeLeaveSummaryDto saveOrUpdateLeaveSummary(String userId, String employeeName,
                                                            int availableLeaves, int takenLeaves,
                                                            int leaveBalance, String updatedBy) {
        EmployeeLeaveSummary summary = employeeLeaveSummaryRepository.findByUserId(userId)
                .orElse(new EmployeeLeaveSummary());

        summary.setUserId(userId);
        summary.setEmployeeName(employeeName);
        summary.setAvailableLeaves(availableLeaves);
        summary.setTakenLeaves(takenLeaves);
        summary.setLeaveBalance(leaveBalance);
        summary.setUpdatedBy(updatedBy);
        summary.setUpdatedAt(LocalDateTime.now());

        EmployeeLeaveSummary savedEntity = employeeLeaveSummaryRepository.save(summary);
        return convertToDto(savedEntity);
    }

    public Optional<EmployeeLeaveSummaryDto> getLeaveSummaryByUserId(String userId) {
        return employeeLeaveSummaryRepository.findByUserId(userId)
                .map(this::convertToDto);
    }

    @Transactional
    public EmployeeLeaveSummaryDto initializeLeaveSummaryForNewEmployee(EmployeeLeaveSummaryDto dto) {
        String userId = dto.getUserId();
        String employeeName = dto.getEmployeeName();
        String employeeType = dto.getEmployeeType();
        LocalDate joiningDate = dto.getJoiningDate();
        String updatedBy = dto.getUpdatedBy();

        if (userId == null || joiningDate == null) return null;

        final int finalAvailableLeaves;
        if ("Full-time".equalsIgnoreCase(employeeType)) {
            int monthsRemaining = 12 - joiningDate.getMonthValue() + 1;
            finalAvailableLeaves = monthsRemaining; // 1 leave per month
        } else {
            finalAvailableLeaves = 0; // For other types like C2C
        }

        Optional<EmployeeLeaveSummary> optionalSummary = employeeLeaveSummaryRepository.findByUserId(userId);

        EmployeeLeaveSummary savedSummary;
        if (optionalSummary.isPresent()) {
            savedSummary = optionalSummary.get();
        } else {
            EmployeeLeaveSummary summary = new EmployeeLeaveSummary();
            summary.setUserId(userId);
            summary.setEmployeeName(employeeName);
            summary.setAvailableLeaves(finalAvailableLeaves);
            summary.setTakenLeaves(0);
            summary.setLeaveBalance(finalAvailableLeaves >= 0 ? finalAvailableLeaves : 0);
            summary.setUpdatedBy(updatedBy);
            summary.setUpdatedAt(LocalDateTime.now());
            savedSummary = employeeLeaveSummaryRepository.save(summary);
        }

        return convertToDto(savedSummary);
    }

    @Transactional
    public EmployeeLeaveSummaryDto updateLeaveOnLeaveTaken(String userId, int leavesTakenNow, String updatedBy) {
        EmployeeLeaveSummary summary = employeeLeaveSummaryRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Leave summary not found for user: " + userId));

        // Update taken leaves cumulatively
        int newTakenLeaves = summary.getTakenLeaves() + leavesTakenNow;

        if (summary.getAvailableLeaves() < 0) {
            // For non-paid leaves (e.g. C2C), set availableLeaves = -1 persistently
            // Only update takenLeaves, leave availableLeaves unchanged
            summary.setTakenLeaves(newTakenLeaves);
            // Optionally, update leaveBalance if needed, or keep zero
            summary.setLeaveBalance(0);
        } else {
            // For paid leaves (Full-time)
            // Subtract newly taken leaves from current availableLeaves
            int newAvailableLeaves = summary.getAvailableLeaves() - leavesTakenNow;
            if (newAvailableLeaves < 0) newAvailableLeaves = 0;

            // leaveBalance = new availableLeaves - total taken leaves; bound to zero
            int newLeaveBalance = newAvailableLeaves - newTakenLeaves;
            if (newLeaveBalance < 0) newLeaveBalance = 0;

            summary.setAvailableLeaves(newAvailableLeaves);
            summary.setTakenLeaves(newTakenLeaves);
            summary.setLeaveBalance(newLeaveBalance);
        }

        summary.setUpdatedBy(updatedBy);
        summary.setUpdatedAt(LocalDateTime.now());

        EmployeeLeaveSummary saved = employeeLeaveSummaryRepository.save(summary);
        return convertToDto(saved);
    }


    private EmployeeLeaveSummaryDto convertToDto(EmployeeLeaveSummary entity) {
        EmployeeLeaveSummaryDto dto = new EmployeeLeaveSummaryDto();
        dto.setUserId(entity.getUserId());
        dto.setEmployeeName(entity.getEmployeeName());
        dto.setAvailableLeaves(entity.getAvailableLeaves());
        dto.setTakenLeaves(entity.getTakenLeaves());
        dto.setLeaveBalance(entity.getLeaveBalance());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }


}
