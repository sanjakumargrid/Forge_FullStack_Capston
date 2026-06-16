package com.talentgrid.auth.jwt;

import com.talentgrid.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    // ==========================================
    // GENERATE ACCESS TOKEN
    // ==========================================

    public String generateToken(User user) {


        return Jwts.builder()
                .subject(String.valueOf(user.getId()))

                .claim("email", user.getEmail())

                .issuer(issuer)

                .id(UUID.randomUUID().toString())

                .claim("token_type", "access")

                .audience()

                .add("api-gateway")

                .and()
                // ADD ROLES
                .claim(
                        "roles",
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .toList()
                )

                // ADD SCOPES
                .claim(
                        "scopes",
                        user.getRoles()
                                .stream()
                                .flatMap(role ->
                                        role.getScopes().stream()
                                )
                                .map(scope -> scope.getName())
                                .distinct()
                                .toList()
                )

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )

                .signWith(getSigningKey())
                .compact();
    }

    // ==========================================
    // GENERATE REFRESH TOKEN
    // ==========================================

    public String generateRefreshToken() {

        return UUID.randomUUID().toString();
    }

    public long getRefreshExpiration() {

        return refreshExpiration;
    }

    // ==========================================
    // EXTRACT EMAIL
    // ==========================================

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .get("email", String.class);
    }

    // ==========================================
    // EXTRACT USER ID
    // ==========================================

    public String extractUserId(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    // ==========================================
    // EXTRACT EXPIRATION
    // ==========================================

    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
    }

    // ==========================================
    // EXTRACT TOKEN TYPE
    // ==========================================

    public String extractTokenType(String token) {

        return extractAllClaims(token)
                .get("token_type", String.class);
    }

    // ==========================================
    // VALIDATE TOKEN
    // ==========================================

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String email = extractEmail(token);

        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    // ==========================================
    // CHECK TOKEN EXPIRATION
    // ==========================================

    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    // ==========================================
    // EXTRACT CLAIM
    // ==========================================

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        final Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    // ==========================================
    // EXTRACT ALL CLAIMS
    // ==========================================

    private Claims extractAllClaims(String token) {

        JwtParser parser = Jwts.parser()
                .verifyWith(getSigningKey())
                .build();

        return parser
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==========================================
    // SIGNING KEY
    // ==========================================

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractJti(String token) {

        return extractAllClaims(token).getId();
    }


}