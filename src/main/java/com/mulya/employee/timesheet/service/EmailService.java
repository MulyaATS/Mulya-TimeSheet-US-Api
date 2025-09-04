package com.mulya.employee.timesheet.service;

import com.mulya.employee.timesheet.model.Timesheet;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private void sendHtmlEmail(String to, String subject, String htmlBody) {

    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("notifications@adroitinnovative.com"); // 👈 force sender
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    } catch (Exception e) {
        System.err.println("Email send failure: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Failed to send email to " + to, e);

    }
}


    // Manager Notification (classic card layout)
    public void sendManagerApprovalRequestEmail(Timesheet ts, String managerEmail, String managerName, String employeeName) {
        String subject = "Timesheet Approval Required – Week Starting " + ts.getWeekStartDate();
        String htmlBody = buildCardTemplate(
                "Timesheet Approval Required",
                "Hi " + managerName + ",<br><br>"
                        + "A new timesheet for <span style='font-weight: bold;'>" + employeeName + "</span> "
                        + "for the period <strong>" + ts.getWeekStartDate() + "</strong> to <strong>" + ts.getWeekEndDate() + "</strong> "
                        + "is awaiting your approval.<br>"
                        + "Please log in to approve or reject.",
                // ✅ Info row now only has Employee Name
                "Employee Name :", employeeName,
                "https://mymulya.com", "Review Timesheet",
                "Thank you,<br>Mulya Team"
        );
        sendHtmlEmail(managerEmail, subject, htmlBody);
    }


    // Employee approval notification
    public void sendEmployeeApprovalEmail(String employeeEmail, String employeeName,
                                          String weekStartDate, String weekEndDate) {
        String subject = "Timesheet Approved";
        String htmlBody = buildCardTemplate(
                "Timesheet Approved",
                "Dear " + employeeName + ",<br><br>"
                        + "Your weekly timesheet for <strong>" + weekStartDate + "</strong> to <strong>" + weekEndDate + "</strong> has been "
                        + "<span style='color:green;font-weight:bold;'>approved</span>.<br>"
                        + "Great job and thank you for your submission!",
                null, null, null, null,
                "Best regards,<br>Mulya Team"
        );
        sendHtmlEmail(employeeEmail, subject, htmlBody);
    }

    // Employee rejection notification
    public void sendEmployeeRejectionEmail(String employeeEmail, String employeeName,
                                           String weekStartDate, String weekEndDate, String reason) {
        String subject = "Timesheet Rejected – Action Required";
        String htmlBody = buildCardTemplate(
                "Timesheet Rejected – Action Required",
                "Dear " + employeeName + ",<br><br>"
                        + "Your weekly timesheet for <strong>" + weekStartDate + "</strong> to <strong>" + weekEndDate + "</strong> has been "
                        + "<span style='color:red;font-weight:bold;'>rejected</span> by the manager.<br>"
                        + "<div style='margin:10px 0 18px 0;border-left:4px solid #ffbdbd;"
                        + "padding-left:12px;background:#fff7f7;color:#b70000;'>"
                        + "<strong>Reason for rejection:</strong><br>" + reason + "</div>"
                        + "Please review the feedback, make necessary changes, and resubmit your timesheet for approval.",
                null, null, null, null,
                "Thank you,<br>Mulya Team"
        );
        sendHtmlEmail(employeeEmail, subject, htmlBody);
    }

    private String buildCardTemplate(String header, String bodyText,
                                     String infoLabel, String infoValue,
                                     String buttonUrl, String buttonText,
                                     String footerText) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>"
                + "body {background: #f5f8fb; font-family: 'Helvetica Neue', Arial, sans-serif; color: #222; margin: 0; padding: 0;}"
                + ".card {background: #fff; max-width: 430px; margin: 42px auto; border-radius: 9px;"
                + " box-shadow: 0 2px 18px rgba(32,82,138,.15), 0 1px 5px rgba(32,82,138,.08);"
                + " padding: 34px 28px 32px 28px; text-align: center; border: 1px solid #e4ebf1;}"
                + ".header {font-size: 1.18rem; font-weight: 600; color: #0056b3; margin-bottom: 23px; letter-spacing: 1px;}"
                + ".body-text {font-size: 1rem; color: #2a3357; margin-bottom: 20px; margin-top: 14px; line-height: 1.5; text-align: left;}"
                + ".info-row {font-size: 1.02rem; color: #2a3357; margin-bottom: 18px; text-align: left;}"
                // --- Updated Button Style ---
                + ".action-btn {background: #ffffff; color: #007bff; font-size: 1.08rem; padding: 12px 38px; border-radius: 7px;"
                + " border: 2px solid #007bff; text-decoration: none; font-weight: 600; display: inline-block; margin-top: 18px;"
                + " transition: all .2s ease-in-out;}"
                + ".action-btn:hover {background: #007bff; color: #ffffff;}"
                // --- End Button Style ---
                + ".footer {font-size: 0.96rem; color: #5e6b8b; margin-top: 30px; text-align: center;}"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'>" + header + "</div>"
                + "<div class='body-text'>" + bodyText + "</div>"
                + (infoLabel != null && infoValue != null
                ? "<div class='info-row'><span style='font-size:0.99rem;'>"
                + infoLabel + "</span> <span style='color:#666;'>" + infoValue + "</span></div>"
                : "")
                + (buttonUrl != null && buttonText != null
                ? "<a href='" + buttonUrl + "' class='action-btn' target='_blank'>" + buttonText + "</a>"
                : "")
                + "<div class='footer'>" + footerText + "</div>"
                + "</div></body></html>";
    }

}
