package com.mulya.employee.timesheet.client;

import com.mulya.employee.timesheet.dto.UserDto;
import com.mulya.employee.timesheet.dto.UserInfoDto;
import com.mulya.employee.timesheet.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class UserRegisterClient {

    @Value("${user.register.service.url}")
    private String userServiceBaseUrl; // e.g., http://USER-SERVICE/api

    @Autowired
    private RestTemplate restTemplate;

    public UserDto getUserById(String userId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(userServiceBaseUrl + "/employee")
                .queryParam("userId", userId)
                .toUriString();

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserDto>>() {}
        );

        List<UserDto> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("User not found with ID: " + userId, ResourceNotFoundException.ResourceType.USER);
        }
        setEmployeeTypeFromRole(users.get(0));
        return users.get(0);
    }

    public List<UserDto> getUsersByRole(String roleName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(userServiceBaseUrl + "/employee")
                .queryParam("roleName", roleName)
                .toUriString();

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserDto>>() {}
        );

        List<UserDto> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("No users found with role: " + roleName, ResourceNotFoundException.ResourceType.USER);
        }
        users.forEach(this::setEmployeeTypeFromRole);
        return users;
    }

    public List<UserInfoDto> getUserInfos(String userIds) {
        String url = userServiceBaseUrl + "/" + userIds + "/username";

        ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        String body = rawResponse.getBody();

        // If response starts with [ or {, assume it's JSON; otherwise handle as plain text
        if (body != null && (body.trim().startsWith("[") || body.trim().startsWith("{"))) {
            // Parse as JSON array
            ResponseEntity<List<UserInfoDto>> jsonResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserInfoDto>>() {}
            );
            return jsonResponse.getBody();
        } else {
            // Convert plain text (like "Sri Ram") into a List<UserInfoDto>
            UserInfoDto dto = new UserInfoDto();
            dto.setUserId(userIds);           // because we passed only one userId
            dto.setUserName(body != null ? body.trim() : "");
            return Collections.singletonList(dto);
        }
    }

    public String getUserEmail(String userId) {
        String url = userServiceBaseUrl + "/" + userId + "/email";
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );
        return response.getBody();
    }

    private void setEmployeeTypeFromRole(UserDto user) {
        if (user.getRole() != null && user.getRole().equalsIgnoreCase("EXTERNALEMPLOYEE")) {
            user.setEmployeeType("EXTERNAL");
        } else {
            user.setEmployeeType("INTERNAL");
        }
    }

    public UserDto getUserNameByRole(String roleName) {
        List<UserDto> users = getUsersByRole(roleName);
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No user found with role: " + roleName, ResourceNotFoundException.ResourceType.USER);
        }
        return users.get(0); // Return the first user found with the specified role
    }
}
