package com.talentgrid.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(
            String jti,
            long expirationMillis
    ) {

        redisTemplate.opsForValue().set(
                "blacklist:" + jti,
                "blacklisted",
                Duration.ofMillis(expirationMillis)
        );
    }

    public boolean isBlacklisted(String jti) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey("blacklist:" + jti)
        );
    }
}