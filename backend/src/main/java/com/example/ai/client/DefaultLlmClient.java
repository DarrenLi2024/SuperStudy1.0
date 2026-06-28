package com.example.ai.client;

import com.example.ai.config.LlmProperties;
import com.example.config.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import javax.annotation.PostConstruct;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Environment environment;
    private RestTemplate restTemplate;

    @PostConstruct
    public void checkConfiguration() {
        if (properties.isRemoteEnabled()) {
            log.info("┌─────────────────────────────────────────────");
            log.info("│ 🤖 AI 大模型模式：远程调用");
            log.info("│     Provider : {}", properties.getProvider());
            log.info("│     Model    : {}", properties.getModel());
            log.info("│     Base URL : {}", properties.getBaseUrl());
            log.info("│     Timeout  : {}ms", properties.getTimeoutMs());
            log.info("│     Cache    : {}", properties.getCacheEnabled() ? "开启" : "关闭");
            log.info("└─────────────────────────────────────────────");
        } else {
            boolean hasKey = properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty();
            boolean hasUrl = properties.getBaseUrl() != null && !properties.getBaseUrl().trim().isEmpty();
            log.warn("┌─────────────────────────────────────────────");
            log.warn("│ ⚠️  AI 大模型模式：本地降级 (local)");
            if (hasKey || hasUrl) {
                log.warn("│     检测到部分配置但 provider=local，需设置");
                log.warn("│     provider 为非 local 值才能启用远程");
            } else {
                log.warn("│     未配置 API 密钥和地址");
            }
            log.warn("│     参考：docs/ai-config-guide.md");
            log.warn("└─────────────────────────────────────────────");
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Math.max(1000, properties.getTimeoutMs()));
            factory.setReadTimeout(Math.max(1000, properties.getTimeoutMs()));
            restTemplate = new RestTemplate(factory);
        }
        return restTemplate;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        String cacheKey = "ai:llm:" + sha256(request.getTaskType() + "|" + request.getUserPrompt());
        if (Boolean.TRUE.equals(properties.getCacheEnabled())) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof String) {
                    return LlmResponse.builder()
                            .content((String) cached)
                            .provider(properties.getProvider())
                            .model(properties.getModel())
                            .cached(true)
                            .fallback(false)
                            .build();
                }
            } catch (Exception e) {
                log.debug("LLM缓存读取跳过: {}", e.getMessage());
            }
        }

        LlmResponse response = properties.isRemoteEnabled()
                ? callRemote(request)
                : localFallback(request);

        if (Boolean.TRUE.equals(properties.getCacheEnabled()) && response.getContent() != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, response.getContent(),
                        Duration.ofSeconds(properties.getCacheTtlSeconds()));
            } catch (Exception e) {
                log.debug("LLM缓存写入跳过: {}", e.getMessage());
            }
        }
        return response;
    }

    private LlmResponse callRemote(LlmRequest request) {
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        RuntimeException lastError = null;
        for (int i = 0; i < attempts; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(properties.getApiKey());

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", properties.getModel());
                body.put("temperature", request.getTemperature() == null ? 0.2 : request.getTemperature());
                body.put("messages", Arrays.asList(
                        message("system", request.getSystemPrompt()),
                        message("user", buildPrompt(request))
                ));

                ResponseEntity<String> entity = getRestTemplate().exchange(
                        properties.getBaseUrl(),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class
                );
                String content = extractContent(entity.getBody());
                if (content == null || content.trim().isEmpty()) {
                    throw new BusinessException(503, "AI服务返回空内容");
                }
                return LlmResponse.builder()
                        .content(content)
                        .provider(properties.getProvider())
                        .model(properties.getModel())
                        .cached(false)
                        .fallback(false)
                        .build();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("LLM调用失败，第{}次: {}", i + 1, e.getMessage());
            }
        }
        throw new BusinessException(503, "AI服务暂时不可用", lastError);
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private String buildPrompt(LlmRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(request.getUserPrompt() == null ? "" : request.getUserPrompt());
        if (request.getResponseSchema() != null && !request.getResponseSchema().isEmpty()) {
            prompt.append("\n\n请严格按以下JSON结构返回，不要输出额外解释：\n")
                    .append(request.getResponseSchema());
        }
        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            prompt.append("\n\n上下文变量：").append(request.getVariables());
        }
        return prompt.toString();
    }

    private String extractContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message").path("content");
                if (!message.isMissingNode()) {
                    return message.asText();
                }
            }
            JsonNode content = root.path("content");
            return content.isMissingNode() ? body : content.asText();
        } catch (Exception e) {
            return body;
        }
    }

    private LlmResponse localFallback(LlmRequest request) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new BusinessException(503, "生产环境未配置真实AI服务");
        }
        return LlmResponse.builder()
                .content("{}")
                .provider("local")
                .model("deterministic-fallback")
                .cached(false)
                .fallback(true)
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
