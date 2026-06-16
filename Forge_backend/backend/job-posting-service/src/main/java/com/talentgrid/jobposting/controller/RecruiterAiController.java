package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.integration.ChatRequest;
import com.talentgrid.jobposting.dto.integration.ChatResponse;
import com.talentgrid.jobposting.integration.AiIntegrationClient;
import com.talentgrid.jobposting.service.PromptGuardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiter-ai")
@RequiredArgsConstructor
public class RecruiterAiController {

    private final PromptGuardService promptGuardService;
    private final AiIntegrationClient aiIntegrationClient;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        String sanitizedMessage = promptGuardService.validateAndSanitize(req.message(), "RECRUITER_CHATBOT");
        ChatRequest sanitizedReq = new ChatRequest(req.sessionId(), sanitizedMessage, req.teamId(), req.featureType());
        return ResponseEntity.ok(aiIntegrationClient.chat(sanitizedReq));
    }
}
