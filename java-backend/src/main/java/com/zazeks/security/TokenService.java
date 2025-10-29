package com.zazeks.security;

import com.zazeks.config.Settings;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Реализация генерации JWT-токена, соответствующая функции
 * {@code create_access_token} из Python-модуля {@code backend/src/security.py}.
 */
public class TokenService {
    private final Settings settings = Settings.getInstance();

    private Key signingKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(settings.getSecretKey());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            byte[] raw = settings.getSecretKey().getBytes(StandardCharsets.UTF_8);
            if (raw.length < 32) {
                raw = Arrays.copyOf(raw, 32);
            }
            return Keys.hmacShaKeyFor(raw);
        }
    }

    public String createAccessToken(Map<String, Object> payload) {
        return createAccessToken(payload, settings.getAccessTokenTtl());
    }

    public String createAccessToken(Map<String, Object> payload, java.time.Duration ttl) {
        Instant now = Instant.now();
        Date expiry = Date.from(now.plus(ttl));
        return Jwts.builder()
                .setClaims(payload)
                .setIssuedAt(Date.from(now))
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.forName(settings.getAlgorithm()))
                .compact();
    }
}
