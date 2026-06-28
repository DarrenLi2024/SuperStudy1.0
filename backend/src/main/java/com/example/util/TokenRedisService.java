package com.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenRedisService {

    private static final String TOKEN_KEY_PREFIX = "login:token:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private Long expiration;

    public void storeToken(Long userId, String token) {
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + userId, token, expiration, TimeUnit.MILLISECONDS);
    }

    public boolean validateToken(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
        return stored != null && stored.equals(token);
    }

    public void removeToken(Long userId) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }
}
