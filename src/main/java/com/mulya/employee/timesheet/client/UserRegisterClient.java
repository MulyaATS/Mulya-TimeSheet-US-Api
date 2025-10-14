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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserRegisterClient {

    @Value("${user.register.service.url}")
    private String userServiceBaseUrl; // e.g., http://USER-SERVICE/api

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Fetch user by userId; throws ResourceNotFoundException if not found.
     */
    public UserDto getUserById(String userId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(userServiceBaseUrl + "/employee")
                .queryParam("userId", userId)
                .toUriString();

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserDto>>() {});

        List<UserDto> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("User not found with ID: " + userId, ResourceNotFoundException.ResourceType.USER);
        }
        setEmployeeTypeFromRole(users.get(0));
        return users.get(0);
    }

    /**
     * Fetch users by role name; throws ResourceNotFoundException if none found.
     */
    public List<UserDto> getUsersByRole(String roleName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(userServiceBaseUrl + "/employee")
                .queryParam("roleName", roleName)
                .toUriString();

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserDto>>() {});

        List<UserDto> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("No users found with role: " + roleName, ResourceNotFoundException.ResourceType.USER);
        }
        users.forEach(this::setEmployeeTypeFromRole);
        return users;
    }

    public UserInfoDto getUserRoleAndUsername(String userId) {
        String url = userServiceBaseUrl + "/usernameByRole/" + userId;

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.isEmpty()) {
                throw new ResourceNotFoundException("User info not found for ID: " + userId, ResourceNotFoundException.ResourceType.USER);
            }

            UserInfoDto dto = new UserInfoDto();
            dto.setUserId(userId);
            dto.setUserName((String) body.get("userName"));
            return dto;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("User not found with ID: " + userId, ResourceNotFoundException.ResourceType.USER);
        }
    }



    /**
     * Fetch user info (userName) by userId(s).
     * Accepts single or multiple comma-separated userIds.
     */
    public List<UserInfoDto> getUserInfos(String userIds) {
        String url = userServiceBaseUrl + "/" + userIds + "/username";

        ResponseEntity<String> rawResponse = restTemplate.exchange(
                url, HttpMethod.GET, null, String.class);

        String body = rawResponse.getBody();

        if (body != null && (body.trim().startsWith("[") || body.trim().startsWith("{"))) {
            // If response is JSON, parse it into List<UserInfoDto>
            ResponseEntity<List<UserInfoDto>> jsonResponse = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<UserInfoDto>>() {});
            List<UserInfoDto> userInfos = jsonResponse.getBody();
            if (userInfos == null || userInfos.isEmpty()) {
                throw new ResourceNotFoundException("No user info found for userIds: " + userIds, ResourceNotFoundException.ResourceType.USER);
            }
            return userInfos;
        } else if (body != null && !body.isBlank()) {
            // Plain text response assumed to be a username for a single userId
            UserInfoDto dto = new UserInfoDto();
            dto.setUserId(userIds.trim());
            dto.setUserName(body.trim());
            return Collections.singletonList(dto);
        } else {
            throw new ResourceNotFoundException("No user info found for userIds: " + userIds, ResourceNotFoundException.ResourceType.USER);
        }
    }

    /**
     * Fetch email by userId.
     * Throws ResourceNotFoundException if user or email not found.
     */
    public String getUserEmail(String userId) {
        String url = userServiceBaseUrl + "/" + userId + "/email";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, String.class);

            String email = response.getBody();
            if (email == null || email.isBlank()) {
                throw new ResourceNotFoundException("Email not found for user ID: " + userId, ResourceNotFoundException.ResourceType.USER);
            }
            return email;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("User not found with ID: " + userId, ResourceNotFoundException.ResourceType.USER);
        }
    }

    /**
     * Returns the first user with the specified role.
     * Throws ResourceNotFoundException if none found.
     */
    public UserDto getUserNameByRole(String roleName) {
        List<UserDto> users = getUsersByRole(roleName);
        return users.get(0);
    }

    private void setEmployeeTypeFromRole(UserDto user) {
        if ("EXTERNALEMPLOYEE".equalsIgnoreCase(user.getRole())) {
            user.setEmployeeType("EXTERNAL");
        } else {
            user.setEmployeeType("INTERNAL");
        }
    }
}
