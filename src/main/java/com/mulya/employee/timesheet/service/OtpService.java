package com.mulya.employee.timesheet.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Map<Long, OtpData> otpStore = new ConcurrentHashMap<>();

    public String generateOTP(Long timesheetId, String email) {
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        otpStore.put(timesheetId, new OtpData(otp, LocalDateTime.now().plusMinutes(10)));
        return otp;
    }

    public boolean validateOTP(Long timesheetId, String otp) {
        OtpData data = otpStore.get(timesheetId);
        if (data != null && data.otp().equals(otp) && data.expiry().isAfter(LocalDateTime.now())) {
            otpStore.remove(timesheetId);
            return true;
        }
        return false;
    }

    private record OtpData(String otp, LocalDateTime expiry) {}
}
