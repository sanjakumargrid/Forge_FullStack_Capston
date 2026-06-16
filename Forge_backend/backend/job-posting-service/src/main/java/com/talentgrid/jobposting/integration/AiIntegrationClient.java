package com.talentgrid.jobposting.integration;

import com.talentgrid.jobposting.dto.integration.ChatRequest;
import com.talentgrid.jobposting.dto.integration.ChatResponse;
import com.talentgrid.jobposting.dto.integration.JdGenerationRequest;
import com.talentgrid.jobposting.dto.integration.JdGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AiIntegrationClient {

    private final RestTemplate restTemplate;

    @Value("${external.ai-service.base-url}")
    private String aiServiceBaseUrl;

    public JdGenerationResponse generateJobDescription(JdGenerationRequest request) {
        String url = aiServiceBaseUrl + "/api/v1/ai/jd/generate";
        return restTemplate.postForObject(url, request, JdGenerationResponse.class);
    }

    public ChatResponse chat(ChatRequest request) {
        String url = aiServiceBaseUrl + "/api/v1/ai/chat";
        return restTemplate.postForObject(url, request, ChatResponse.class);
    }
}
