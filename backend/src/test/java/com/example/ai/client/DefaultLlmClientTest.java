package com.example.ai.client;

import com.example.ai.config.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultLlmClientTest {

    @Test
    void generate_withoutRemoteConfig_returnsLocalFallback() {
        LlmProperties properties = new LlmProperties();
        properties.setCacheEnabled(false);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        DefaultLlmClient client = new DefaultLlmClient(properties, new ObjectMapper(), redisTemplate, environment);

        LlmResponse response = client.generate(LlmRequest.builder()
                .taskType("test")
                .userPrompt("hello")
                .build());

        assertTrue(response.isFallback());
        assertEquals("local", response.getProvider());
        assertEquals("{}", response.getContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_withCacheHit_returnsCachedContent() {
        LlmProperties properties = new LlmProperties();
        properties.setCacheEnabled(true);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(redisTemplate.opsForValue()).thenReturn(ops);
        // getCached() 期望返回值是 Map 类型
        Map<String, Object> cachedMap = new LinkedHashMap<>();
        cachedMap.put("content", "{\"ok\":true}");
        when(ops.get(anyString())).thenReturn(cachedMap);
        DefaultLlmClient client = new DefaultLlmClient(properties, new ObjectMapper(), redisTemplate, environment);

        LlmResponse response = client.generate(LlmRequest.builder()
                .taskType("test")
                .userPrompt("hello")
                .build());

        assertTrue(response.isCached());
        assertEquals("{\"ok\":true}", response.getContent());
    }
}
