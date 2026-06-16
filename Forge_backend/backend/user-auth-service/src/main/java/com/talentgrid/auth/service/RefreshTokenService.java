package com.talentgrid.auth.service;

import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtService jwtService;

    public RefreshToken createRefreshToken(User user) {

        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtService.generateRefreshToken())
                .user(user)
                .expiryDate(
                        LocalDateTime.now()
                                .plusSeconds(
                                        jwtService.getRefreshExpiration() / 1000
                                )
                )
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() ->
                        new RuntimeException("Invalid refresh token")
                );

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }


    public void revokeRefreshToken(User user) {

        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);
    }

    public void deleteByToken(String token) {

        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(
            RefreshToken existingToken
    ) {

        existingToken.setToken(
                jwtService.generateRefreshToken()
        );

        return refreshTokenRepository.save(existingToken);
    }

}
