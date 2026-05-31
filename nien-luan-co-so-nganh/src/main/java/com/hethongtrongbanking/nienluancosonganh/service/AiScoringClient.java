package com.hethongtrongbanking.nienluancosonganh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.hethongtrongbanking.nienluancosonganh.dto.request.PaymentRequest;
import com.hethongtrongbanking.nienluancosonganh.dto.response.AiScoreResponse;

import lombok.extern.slf4j.Slf4j;

// Noi giua Spring Boot va Python AI Service
// Duty: Gui data Giao dich -> nhan risk score
// Khong luu DB, khong quyet dinh Approved/Blocked (cai nay cua PaymentService)

@Component
@Slf4j
public class AiScoringClient {

    // Doc URL tu application.properties
    @Value("${ai.service.url}")
    private String aiServiceUrl;

    // Tao RestTemplate 1 lan duy nhat, khong build lai moi request
    private final RestTemplate restTemplate;

    public AiScoringClient() {
        this.restTemplate = new RestTemplate();
    }

    public AiScoreResponse score(PaymentRequest request) {
        log.info("Calling AI service: {}", aiServiceUrl);

        try {
            ResponseEntity<AiScoreResponse> response =
                    restTemplate.postForEntity(aiServiceUrl, request, AiScoreResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("AI service returned empty or error status");
            }

            AiScoreResponse body = response.getBody();
            log.info("AI result: riskScore = {}, riskLevel = {}", body.getRiskScore(), body.getRiskLevel());
            return body;

        } catch (RestClientException e) {
            // Connection Error
            log.error("Cannot reach AI service at {}: {}", aiServiceUrl, e.getMessage());
            throw new RuntimeException("AI scoring service unavailable", e);
        }
    }
}
