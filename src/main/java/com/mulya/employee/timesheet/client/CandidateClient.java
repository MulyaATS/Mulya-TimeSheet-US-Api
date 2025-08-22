package com.mulya.employee.timesheet.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulya.employee.timesheet.dto.PlacementDetailsDto;
import com.mulya.employee.timesheet.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class CandidateClient {

    @Value("${candidate.service.url}")
    private String candidateServiceBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    public List<PlacementDetailsDto> getPlacementsByEmail(String candidateEmailId) {
        String url = UriComponentsBuilder.fromHttpUrl(candidateServiceBaseUrl + "/placement/placements-list")
                .queryParam("email", candidateEmailId)
                .toUriString();
        System.out.println("Candidate service URL called: " + url);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = response.getBody();

        List<PlacementDetailsDto> placements = null;
        if (body != null && body.containsKey("data")) {
            Object dataObj = body.get("data");
            placements = mapper.convertValue(dataObj, new TypeReference<List<PlacementDetailsDto>>() {});
        }

        if (placements == null || placements.isEmpty()) {
            throw new ResourceNotFoundException("No placement details found for candidate email: " + candidateEmailId);
        }

        return placements;
    }
}