package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.Attachment;
import com.mulya.employee.timesheet.model.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository  extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByUserId(String userId);

    Optional<Timesheet> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);

    @Query(value = "SELECT timesheet_id FROM timesheets ORDER BY timesheet_id DESC LIMIT 1", nativeQuery = true)
    String findMaxTimesheetId();

    Optional<Timesheet> findByTimesheetId(String timesheetId);

    Page<Timesheet> findByStatus(String status, Pageable pageable);

    List<Timesheet> findByWeekStartDateBetween(LocalDate startDate, LocalDate endDate);


    @Query("SELECT t FROM Timesheet t WHERE t.userId = :userId " +
            "AND (t.weekStartDate BETWEEN :monthStart AND :monthEnd " +
            "OR (t.weekStartDate < :monthStart AND t.weekEndDate >= :monthStart))")
    List<Timesheet> findTimesheetsOverlappingMonth(
            @Param("userId") String userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);


}