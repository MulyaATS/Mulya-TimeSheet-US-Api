package com.mulya.employee.timesheet.repository;

import com.mulya.employee.timesheet.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTimesheetTimesheetId(String timesheetId);
}
