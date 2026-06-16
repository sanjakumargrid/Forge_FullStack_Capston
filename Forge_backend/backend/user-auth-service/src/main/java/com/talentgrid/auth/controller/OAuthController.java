package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.UserContextDto;
import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/oauth-success")
    public ResponseEntity<LoginResponse> oauthSuccess(@RequestParam String email) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Role candidateRole = roleRepository.findByName("CANDIDATE")
                            .orElseThrow(() -> new RuntimeException("CANDIDATE role not found"));

                    return userRepository.save(User.builder()
                            .username(email.split("@")[0])
                            .email(email)
                            .password("OAUTH_USER")
                            .enabled(true)
                            .roles(Set.of(candidateRole))
                            .build());
                });

        String accessToken = jwtService.generateToken(user);
        refreshTokenService.createRefreshToken(user);

        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String primaryRole = roleNames.isEmpty() ? "EMPLOYEE" : roleNames.get(0);

        UserContextDto userCtx = UserContextDto.builder()
                .id(user.getId())
                .name(user.getUsername())
                .email(user.getEmail())
                .role(primaryRole)
                .roles(roleNames)
                .permissions(List.of())
                .businessUnit(null)
                .build();

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900)
                .user(userCtx)
                .build());
    }
}
