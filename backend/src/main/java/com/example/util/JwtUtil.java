package com.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET 未设置！生产环境必须通过环境变量 JWT_SECRET 注入一个至少 64 字符的随机密钥。"
            );
        }
        if (secret.length() < 64) {
            throw new IllegalStateException(
                "JWT_SECRET 长度不足 " + secret.length() + " 字符，HS512 要求至少 64 字符。"
            );
        }
        log.info("JWT 密钥校验通过，长度: {} 字符", secret.length());
    }

    private SecretKey getSigningKey() {
        // HS512 要求密钥至少 512 bits = 64 bytes
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, String role, Long studentId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (studentId != null) {
            claims.put("studentId", studentId);
        }
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object userId = claims.get("userId");
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        }
        return null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object role = claims.get("role");
            return role != null ? role.toString() : null;
        }
        return null;
    }

    public Long getStudentIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object studentId = claims.get("studentId");
            return studentId != null ? Long.valueOf(studentId.toString()) : null;
        }
        return null;
    }

    public Boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        }
        return true;
    }

    public Boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
