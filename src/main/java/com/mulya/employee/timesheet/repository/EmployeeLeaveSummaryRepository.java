package com.mulya.employee.timesheet.repository;
import com.mulya.employee.timesheet.model.EmployeeLeaveSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeLeaveSummaryRepository extends JpaRepository<EmployeeLeaveSummary, Long> {
    Optional<EmployeeLeaveSummary> findByUserId(String userId);

    List<EmployeeLeaveSummary> findByUserIdIn(Collection<String> userIds);

}
