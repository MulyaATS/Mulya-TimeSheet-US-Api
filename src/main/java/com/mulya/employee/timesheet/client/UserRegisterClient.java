package com.mulya.employee.timesheet.client;

import com.mulya.employee.timesheet.dto.UserDto;
import com.mulya.employee.timesheet.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class UserRegisterClient {

    @Value("${user.register.service.url}")  // e.g. http://localhost:8083/users/employee
    private String userServiceBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    public UserDto getUserById(String userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .queryParam("userId", userId)
                    .toUriString();

            ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
            );

            List<UserDto> users = response.getBody();

            if (users == null || users.isEmpty()) {
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }

            UserDto user = users.get(0);

            // Derive type from roles or other logic; example:
            String roles = user.getEmployeeType();
            if (roles != null && roles.toUpperCase().contains("EXTERNAL")) {
                user.setEmployeeType("EXTERNAL");
            } else {
                user.setEmployeeType("INTERNAL");
            }

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user from user register service", e);
        }
    }
}
