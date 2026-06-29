package com.example.ai.client;

import com.example.ai.config.LlmProperties;
import com.example.config.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Environment environment;
    private RestTemplate restTemplate;
    private RestTemplate streamTemplate;

    /** 用于从LLM文本中提取JSON的正则 */
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]");

    @PostConstruct
    public void checkConfiguration() {
        if (properties.isRemoteEnabled()) {
            log.info("┌─────────────────────────────────────────────");
            log.info("│ 🤖 AI 大模型模式：远程调用");
            log.info("│     Provider : {}", properties.getProvider());
            log.info("│     Model    : {}", properties.getModel());
            log.info("│     Base URL : {}", properties.getBaseUrl());
            log.info("│     Timeout  : {}ms", properties.getTimeoutMs());
            log.info("│     Stream   : {}", properties.getStreamEnabled() ? "开启" : "关闭");
            log.info("│     Cache    : {}", properties.getCacheEnabled() ? "开启" : "关闭");
            log.info("└─────────────────────────────────────────────");
        } else {
            log.warn("┌─────────────────────────────────────────────");
            log.warn("│ ⚠️  AI 大模型模式：本地降级 (local)");
            log.warn("│     设置 AI_LLM_PROVIDER 环境变量以启用远程AI");
            log.warn("│     参考：docs/ai-config-guide.md");
            log.warn("└─────────────────────────────────────────────");
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = createRestTemplate(properties.getTimeoutMs());
        }
        return restTemplate;
    }

    private RestTemplate getStreamTemplate() {
        if (streamTemplate == null) {
            // 流式需要更长超时
            streamTemplate = createRestTemplate(Math.max(properties.getTimeoutMs(), 120000));
        }
        return streamTemplate;
    }

    private RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1000, timeoutMs));
        factory.setReadTimeout(Math.max(1000, timeoutMs));
        return new RestTemplate(factory);
    }

    // ==================== 同步生成 ====================

    @Override
    public LlmResponse generate(LlmRequest request) {
        long start = System.currentTimeMillis();

        // 缓存检查
        if (!request.isSkipCache() && Boolean.TRUE.equals(properties.getCacheEnabled())) {
            LlmResponse cached = getCached(request);
            if (cached != null) return cached;
        }

        // 调用LLM
        LlmResponse response;
        if (properties.isRemoteEnabled()) {
            response = callRemote(request);
        } else {
            response = localFallback(request);
        }

        // 写入缓存
        if (!request.isSkipCache()
                && Boolean.TRUE.equals(properties.getCacheEnabled())
                && response.getContent() != null
                && !response.isFallback()) {
            setCache(request, response);
        }

        response.setElapsedMs(System.currentTimeMillis() - start);
        return response;
    }

    // ==================== 流式生成 ====================

    @Override
    public LlmResponse generateStream(LlmRequest request, Consumer<String> onToken) {
        long start = System.currentTimeMillis();

        if (!properties.isRemoteEnabled()) {
            // 降级：直接返回fallback内容
            LlmResponse fallback = localFallback(request);
            if (onToken != null && fallback.getContent() != null) {
                onToken.accept(fallback.getContent());
            }
            fallback.setElapsedMs(System.currentTimeMillis() - start);
            return fallback;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            Map<String, Object> body = buildRequestBody(request);
            body.put("stream", true);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 使用底层HttpURLConnection进行SSE流式读取
            java.net.URL url = new java.net.URL(properties.getBaseUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            conn.setDoOutput(true);
            conn.setConnectTimeout(Math.max(1000, properties.getTimeoutMs()));
            conn.setReadTimeout(Math.max(1000, Math.max(properties.getTimeoutMs(), 120000)));

            // 写入请求体
            String requestBody = objectMapper.writeValueAsString(body);
            try (var os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 读取SSE流
            StringBuilder fullContent = new StringBuilder();
            int status = conn.getResponseCode();
            if (status >= 400) {
                throw new BusinessException(503, "AI服务流式请求失败，状态码：" + status);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode choices = node.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta").path("content");
                                if (!delta.isMissingNode()) {
                                    String token = delta.asText();
                                    fullContent.append(token);
                                    if (onToken != null) onToken.accept(token);
                                }
                            }
                        } catch (Exception ignored) {
                            // 跳过非JSON行
                        }
                    }
                }
            }
            conn.disconnect();

            String content = fullContent.toString();
            LlmResponse response = LlmResponse.builder()
                    .content(content)
                    .structured(tryParseStructured(content, request.getResponseSchema()))
                    .provider(properties.getProvider())
                    .model(properties.getModel())
                    .cached(false)
                    .fallback(false)
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build();

            log.info("LLM流式调用完成 [{}] 耗时{}ms 输出{}字符",
                    request.getTaskType(), response.getElapsedMs(), content.length());
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM流式调用失败 [{}]: {}", request.getTaskType(), e.getMessage());
            LlmResponse fallback = localFallback(request);
            fallback.setElapsedMs(System.currentTimeMillis() - start);
            if (onToken != null && fallback.getContent() != null) {
                onToken.accept(fallback.getContent());
            }
            return fallback;
        }
    }

    // ==================== 远程调用 ====================

    private LlmResponse callRemote(LlmRequest request) {
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        RuntimeException lastError = null;

        for (int i = 0; i < attempts; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(properties.getApiKey());

                Map<String, Object> body = buildRequestBody(request);
                body.put("stream", false);

                ResponseEntity<String> entity = getRestTemplate().exchange(
                        properties.getBaseUrl(),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class
                );

                String rawContent = extractContent(entity.getBody());
                if (rawContent == null || rawContent.trim().isEmpty()) {
                    throw new BusinessException(503, "AI服务返回空内容");
                }

                // 提取并清理JSON内容
                String content = cleanContent(rawContent);
                Map<String, Object> structured = tryParseStructured(content, request.getResponseSchema());

                LlmResponse.TokenUsage usage = extractUsage(entity.getBody());

                LlmResponse response = LlmResponse.builder()
                        .content(content)
                        .structured(structured)
                        .provider(properties.getProvider())
                        .model(properties.getModel())
                        .cached(false)
                        .fallback(false)
                        .usage(usage)
                        .build();

                log.info("LLM调用成功 [{}] model={} tokens={}",
                        request.getTaskType(), properties.getModel(),
                        usage.getTotalTokens());
                return response;

            } catch (BusinessException e) {
                throw e;
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("LLM调用失败 [{}] 第{}/{}次: {}", request.getTaskType(), i + 1, attempts, e.getMessage());
                if (i < attempts - 1) {
                    try { Thread.sleep(500L * (i + 1)); } catch (InterruptedException ignored) {}
                }
            }
        }
        throw new BusinessException(503, "AI服务暂时不可用，已重试" + attempts + "次", lastError);
    }

    // ==================== 请求构建 ====================

    private Map<String, Object> buildRequestBody(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.2);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048);

        // 构建消息列表：system + few-shot examples + user
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System prompt
        String systemPrompt = buildSystemPrompt(request);
        messages.add(message("system", systemPrompt));

        // 2. Few-shot examples
        if (request.getExamples() != null) {
            for (LlmRequest.FewShotExample example : request.getExamples()) {
                messages.add(message("user", example.getUser()));
                messages.add(message("assistant", example.getAssistant()));
            }
        }

        // 3. User prompt
        messages.add(message("user", buildUserPrompt(request)));

        body.put("messages", messages);
        return body;
    }

    private String buildSystemPrompt(LlmRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getSystemPrompt() != null ? request.getSystemPrompt() : "");

        // 增强约束：输出格式、安全规范
        sb.append("\n\n## 输出约束\n");
        sb.append("- 严格按要求的JSON格式输出，不要添加额外解释\n");
        sb.append("- 内容必须符合中国高中教育大纲，不得出现政治敏感、暴力、色情等内容\n");
        sb.append("- 若题目涉及具体数值，确保数值合理可验证\n");
        sb.append("- 语言为简体中文\n");

        if (request.getResponseSchema() != null && !request.getResponseSchema().isEmpty()) {
            sb.append("\n## 期望输出格式\n```json\n")
                    .append(request.getResponseSchema())
                    .append("\n```\n");
        }
        return sb.toString();
    }

    private String buildUserPrompt(LlmRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getUserPrompt() != null ? request.getUserPrompt() : "");

        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            sb.append("\n\n## 上下文数据\n");
            for (Map.Entry<String, Object> entry : request.getVariables().entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content == null ? "" : content);
        return msg;
    }

    // ==================== 响应解析 ====================

    private String extractContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message").path("content");
                if (!msg.isMissingNode()) return msg.asText();
            }
            JsonNode content = root.path("content");
            return content.isMissingNode() ? body : content.asText();
        } catch (Exception e) {
            return body;
        }
    }

    private LlmResponse.TokenUsage extractUsage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                return new LlmResponse.TokenUsage(
                        usage.path("prompt_tokens").asInt(0),
                        usage.path("completion_tokens").asInt(0),
                        usage.path("total_tokens").asInt(0)
                );
            }
        } catch (Exception ignored) {}
        return new LlmResponse.TokenUsage();
    }

    /**
     * 清理LLM返回内容：提取markdown代码块中的JSON、去除前后空白
     */
    private String cleanContent(String raw) {
        if (raw == null) return "";
        String content = raw.trim();

        // 尝试提取```json ... ```代码块
        Matcher matcher = JSON_BLOCK.matcher(content);
        if (matcher.find()) {
            content = matcher.group(1).trim();
        }
        return content;
    }

    /**
     * 尝试将文本解析为结构化Map
     */
    private Map<String, Object> tryParseStructured(String content, String responseSchema) {
        if (content == null || content.isEmpty() || "{}".equals(content)) {
            return new LinkedHashMap<>();
        }
        try {
            // 先尝试数组
            if (content.trim().startsWith("[")) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("items", objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {}));
                return wrapper;
            }
            // 尝试对象
            if (content.trim().startsWith("{")) {
                return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
            // 解析失败返回空Map，保留原始content
        }
        return new LinkedHashMap<>();
    }

    // ==================== 缓存 ====================

    private LlmResponse getCached(LlmRequest request) {
        String cacheKey = buildCacheKey(request);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) cached;
                return LlmResponse.builder()
                        .content((String) map.getOrDefault("content", ""))
                        .provider(properties.getProvider())
                        .model(properties.getModel())
                        .cached(true)
                        .fallback(false)
                        .build();
            }
        } catch (Exception e) {
            log.debug("LLM缓存读取跳过: {}", e.getMessage());
        }
        return null;
    }

    private void setCache(LlmRequest request, LlmResponse response) {
        String cacheKey = buildCacheKey(request);
        try {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("content", response.getContent());
            value.put("taskType", request.getTaskType());
            redisTemplate.opsForValue().set(cacheKey, value,
                    Duration.ofSeconds(properties.getCacheTtlSeconds()));
        } catch (Exception e) {
            log.debug("LLM缓存写入跳过: {}", e.getMessage());
        }
    }

    private String buildCacheKey(LlmRequest request) {
        // 缓存key使用taskType + 规范化后的userPrompt（去除空白差异）
        String normalizedPrompt = request.getUserPrompt() != null
                ? request.getUserPrompt().replaceAll("\\s+", " ").trim()
                : "";
        return "ai:llm:v2:" + sha256(request.getTaskType() + "|" + normalizedPrompt);
    }

    // ==================== 降级 ====================

    private LlmResponse localFallback(LlmRequest request) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new BusinessException(503, "生产环境未配置真实AI服务，请设置AI_LLM_PROVIDER等环境变量");
        }
        log.debug("LLM本地降级 [{}] provider=local", request.getTaskType());
        return LlmResponse.builder()
                .content("{}")
                .provider("local")
                .model("deterministic-fallback")
                .cached(false)
                .fallback(true)
                .build();
    }

    // ==================== 工具方法 ====================

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
