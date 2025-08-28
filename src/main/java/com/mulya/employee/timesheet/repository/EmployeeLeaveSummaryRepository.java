package com.mulya.employee.timesheet.repository;
import com.mulya.employee.timesheet.model.EmployeeLeaveSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeLeaveSummaryRepository extends JpaRepository<EmployeeLeaveSummary, Long> {
    Optional<EmployeeLeaveSummary> findByUserId(String userId);
}
