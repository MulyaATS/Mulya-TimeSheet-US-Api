package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository  extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByUserId(String userId);

    Optional<Timesheet> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);

    @Query(value = "SELECT timesheet_id FROM timesheets ORDER BY timesheet_id DESC LIMIT 1", nativeQuery = true)
    String findMaxTimesheetId();
}