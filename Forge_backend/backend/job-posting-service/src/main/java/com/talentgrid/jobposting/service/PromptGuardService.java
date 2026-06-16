package com.talentgrid.jobposting.service;

import com.gridynamics.forge.guardrail.GuardrailContext;
import com.gridynamics.forge.guardrail.GuardrailEngine;
import com.gridynamics.forge.guardrail.GuardrailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptGuardService {

    private final GuardrailEngine guardrailEngine;

    public String validateAndSanitize(String rawPrompt, String contextKey) {
        if (rawPrompt == null) {
            return null;
        }
        GuardrailContext context = GuardrailContext.of(contextKey, "BACKEND");
        GuardrailResult result = guardrailEngine.evaluate(rawPrompt, context);
        if (!result.isAllowed()) {
            throw new IllegalArgumentException("Prompt was blocked by security guardrail");
        }
        return result.getSanitisedPrompt();
    }
}
