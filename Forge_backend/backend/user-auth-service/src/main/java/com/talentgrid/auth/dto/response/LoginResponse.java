package com.talentgrid.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Mirrors the frontend AuthResponse TypeScript interface exactly.
 */
@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private long expiresIn;
    private UserContextDto user;
}
