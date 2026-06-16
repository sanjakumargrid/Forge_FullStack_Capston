package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.LoginRequest;
import com.talentgrid.auth.dto.request.RegisterRequest;
import com.talentgrid.auth.dto.response.LoginResponse;
import com.talentgrid.auth.dto.response.RegisterResponse;
import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.Role;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtBlacklistService;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.RoleRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.RefreshTokenService;
import com.talentgrid.auth.service.interfaces.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        String requestedRole =
                request.getRole() != null ? request.getRole() : "EMPLOYEE";

        Role role = roleRepository.findByName(requestedRole.toUpperCase())
                .orElseThrow(() ->
                        new RuntimeException("Role not found: " + requestedRole)
                );

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(role))
                .failedAttempts(0)
                .accountLocked(false)
                .build();

        userRepository.save(user);

        return RegisterResponse.builder()
                .message("User registered successfully")
                .email(user.getEmail())
                .role(role.getName())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // ==============================
        // ACCOUNT LOCK CHECK
        // ==============================
        if (Boolean.TRUE.equals(user.getAccountLocked())) {

            if (user.getLockTime() != null &&
                    user.getLockTime()
                            .plusMinutes(LOCK_DURATION_MINUTES)
                            .isBefore(LocalDateTime.now())) {

                user.setAccountLocked(false);
                user.setFailedAttempts(0);
                user.setLockTime(null);

                userRepository.save(user);

            } else {
                throw new RuntimeException("Account locked. Try again later.");
            }
        }

        // ==============================
        // PASSWORD VALIDATION
        // ==============================
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
            attempts++;

            user.setFailedAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            userRepository.save(user);

            throw new BadCredentialsException("Invalid email or password");
        }

        // ==============================
        // SUCCESS LOGIN RESET
        // ==============================
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        // ==============================
        // JWT + REFRESH TOKEN
        // ==============================
        String accessToken = jwtService.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return buildLoginResponse(accessToken, user);
    }

//    @Override
//    public LoginResponse refreshToken(String refreshToken) {
//
//        RefreshToken storedToken =
//                refreshTokenService.validateRefreshToken(refreshToken);
//
//        User user = storedToken.getUser();
//
//        String accessToken = jwtService.generateToken(user);
//
//        return LoginResponse.builder()
//                .accessToken(accessToken)
//                .type("Bearer")
//                .email(user.getEmail())
//                .roles(user.getRoles()
//                        .stream()
//                        .map(Role::getName)
//                        .collect(Collectors.toSet()))
//                .build();
//    }

    @Override
    public LoginResponse refreshToken(
            String refreshToken,
            HttpServletResponse response
    ) {

        // validate old refresh token
        RefreshToken storedToken =
                refreshTokenService.validateRefreshToken(refreshToken);

        User user = storedToken.getUser();

        // ROTATE TOKEN
        RefreshToken newRefreshToken =
                refreshTokenService.rotateRefreshToken(storedToken);

        // generate new access token
        String accessToken = jwtService.generateToken(user);

        // create NEW cookie
        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", newRefreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return buildLoginResponse(accessToken, user);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private LoginResponse buildLoginResponse(String accessToken, User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String primaryRole = roleNames.isEmpty() ? "EMPLOYEE" : roleNames.get(0);

        com.talentgrid.auth.dto.response.UserContextDto userCtx =
                com.talentgrid.auth.dto.response.UserContextDto.builder()
                        .id(user.getId())
                        .name(user.getUsername())
                        .email(user.getEmail())
                        .role(primaryRole)
                        .roles(roleNames)
                        .permissions(derivePermissions(roleNames))
                        .businessUnit(null)
                        .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900)
                .user(userCtx)
                .build();
    }

    private List<String> derivePermissions(List<String> roles) {
        List<String> perms = new java.util.ArrayList<>();
        for (String role : roles) {
            switch (role) {
                case "RECRUITER" -> { perms.add("posting:create"); perms.add("posting:edit"); perms.add("posting:publish"); perms.add("posting:read"); }
                case "HIRING_MANAGER" -> { perms.add("posting:approve"); perms.add("posting:read"); }
                case "ADMIN" -> perms.add("*");
                default -> perms.add("posting:read");
            }
        }
        return perms;
    }
}