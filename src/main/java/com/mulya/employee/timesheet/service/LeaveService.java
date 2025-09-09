package com.mulya.employee.timesheet.service;

import com.mulya.employee.timesheet.dto.EmployeeLeaveSummaryDto;
import com.mulya.employee.timesheet.model.EmployeeLeaveSummary;
import com.mulya.employee.timesheet.repository.EmployeeLeaveSummaryRepository;
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
            summary.setUpdatedBy(updatedBy);
            summary.setUpdatedAt(LocalDateTime.now());
            savedSummary = employeeLeaveSummaryRepository.save(summary);
        }

        return convertToDto(savedSummary);
    }

    @Transactional
    public EmployeeLeaveSummaryDto updateLeaveOnLeaveTaken(String userId, int newLeavesTaken, String updatedBy) {
        EmployeeLeaveSummary summary = employeeLeaveSummaryRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Leave summary not found for user: " + userId));

        int prevTakenLeaves = summary.getTakenLeaves() != null ? summary.getTakenLeaves() : 0;
        int updatedTakenLeaves = prevTakenLeaves + newLeavesTaken;

        summary.setTakenLeaves(updatedTakenLeaves);

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
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }


}
