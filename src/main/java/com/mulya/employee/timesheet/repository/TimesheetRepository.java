package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimesheetRepository  extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByUserId(String userId);

    Optional<Timesheet> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);
}