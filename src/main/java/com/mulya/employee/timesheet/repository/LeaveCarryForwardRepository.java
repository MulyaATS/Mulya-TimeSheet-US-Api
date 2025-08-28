package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.LeaveCarryForward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveCarryForwardRepository extends JpaRepository<LeaveCarryForward, String> {
    // The String is the type of userId (primary key)
}
