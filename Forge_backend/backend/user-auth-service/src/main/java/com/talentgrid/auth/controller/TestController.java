package com.talentgrid.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * GENERAL AUTH TEST
     */
    @GetMapping("/secure")
    public ResponseEntity<Map<String, Object>> secureApi(
            Authentication authentication
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put(
                "message",
                "JWT authentication successful"
        );

        response.put(
                "loggedInUser",
                authentication.getName()
        );

        response.put(
                "authorities",
                authentication.getAuthorities()
        );

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ROLE + SCOPE TEST
     */
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminApi(
            Authentication authentication
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put(
                "message",
                "RBAC WORKING - ADMIN ACCESS GRANTED"
        );

        response.put(
                "loggedInUser",
                authentication.getName()
        );

        response.put(
                "authorities",
                authentication.getAuthorities()
        );

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}