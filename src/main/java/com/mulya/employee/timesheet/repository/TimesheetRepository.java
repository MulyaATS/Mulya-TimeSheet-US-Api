package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimesheetRepository  extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByUserId(String userId);
}