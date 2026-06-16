package com.talentgrid.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Mirrors the frontend UserContext TypeScript interface exactly.
 */
@Getter
@Builder
public class UserContextDto {
    private long id;
    private String name;
    private String email;
    private String role;
    private List<String> roles;
    private List<String> permissions;
    private String businessUnit;
}
