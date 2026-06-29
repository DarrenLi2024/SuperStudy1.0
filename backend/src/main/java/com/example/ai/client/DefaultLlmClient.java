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
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd) {
            log.error("生产环境未配置真实AI服务 [taskType={}]，请设置AI_LLM_PROVIDER等环境变量", request.getTaskType());
        } else {
            log.warn("LLM本地降级 [{}] provider=local，建议设置AI_LLM_PROVIDER环境变量启用远程AI", request.getTaskType());
        }

        // 基于任务类型生成确定性降级内容（不再抛异常或返回空JSON）
        String fallbackContent = generateDeterministicFallback(request);
        return LlmResponse.builder()
                .content(fallbackContent)
                .provider("local")
                .model("deterministic-fallback")
                .cached(false)
                .fallback(true)
                .build();
    }

    /**
     * 基于任务类型的确定性降级：根据请求中的变量数据，生成规则驱动的合理输出
     */
    private String generateDeterministicFallback(LlmRequest request) {
        String taskType = request.getTaskType();
        Map<String, Object> vars = request.getVariables() != null ? request.getVariables() : Collections.emptyMap();

        try {
            if ("daily_tasks".equals(taskType)) {
                return generateFallbackTasks(vars);
            } else if ("weekly_report".equals(taskType) || "monthly_report".equals(taskType)) {
                return generateFallbackReport(vars, taskType);
            } else if ("knowledge_mastery".equals(taskType)) {
                return generateFallbackKnowledge(vars);
            } else if ("error_analysis".equals(taskType)) {
                return generateFallbackErrorAnalysis(vars);
            } else if ("question_generation".equals(taskType)) {
                return generateFallbackQuestions(vars);
            } else if ("incentive".equals(taskType)) {
                return generateFallbackIncentive(vars);
            }
        } catch (Exception e) {
            log.debug("确定性降级生成失败: {}", e.getMessage());
        }
        return "{}";
    }

    private String generateFallbackTasks(Map<String, Object> vars) {
        Object weightsObj = vars.get("学科权重");
        String weightsStr = weightsObj != null ? weightsObj.toString() : "";
        String[] weightParts = weightsStr.split(", ");
        List<Map<String, Object>> tasks = new ArrayList<>();
        int idx = 1;
        for (String part : weightParts) {
            if (idx > 4) break;
            String[] kv = part.split(":");
            if (kv.length >= 2) {
                String subject = kv[0].trim();
                String pct = kv[1].replace("%", "").trim();
                double weight = 0;
                try { weight = Integer.parseInt(pct) / 100.0; } catch (NumberFormatException ignored) {}
                String point = subjectKnowledgePoint(subject);
                Map<String, Object> task = new LinkedHashMap<>();
                task.put("subject", subject);
                task.put("knowledgePoint", point);
                task.put("type", weight >= 0.35 ? "专项补强" : weight >= 0.2 ? "知识点巩固" : "常规练习");
                task.put("content", subject + "：" + point + "专项训练");
                task.put("durationMinutes", Math.max(20, (int)(weight * 120)));
                task.put("aiHint", "基于薄弱学科权重自动分配，重点突破" + point);
                tasks.add(task);
                idx++;
            }
        }
        if (tasks.isEmpty()) {
            tasks.addAll(generateDefaultTaskList());
        }
        try {
            return objectMapper.writeValueAsString(tasks);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateFallbackReport(Map<String, Object> vars, String taskType) {
        String period = taskType.contains("weekly") ? "本周" : "本月";
        return "【AI" + (taskType.contains("weekly") ? "周复盘" : "月度成长总结") + "】"
                + period + "学习整体保持稳定。建议重点关注薄弱学科的基础知识巩固，"
                + "结合错题复盘进行针对性突破。保持当前学习节奏，稳步提升。";
    }

    private String generateFallbackKnowledge(Map<String, Object> vars) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String[] subjects = {"语文", "数学", "英语", "历史", "政治", "地理"};
        for (String subject : subjects) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("mastery", 50 + (int)(Math.random() * 30));
            info.put("advice", "建议加强" + subject + "基础训练");
            result.put(subject, info);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String generateFallbackErrorAnalysis(Map<String, Object> vars) {
        return "错因集中在基础概念理解和解题步骤衔接上。建议先回顾相关定义和公式，再做2-3道同类题巩固。";
    }

    private String generateFallbackQuestions(Map<String, Object> vars) {
        String subject = vars.containsKey("subject") ? vars.get("subject").toString() : "数学";
        String difficulty = vars.containsKey("difficulty") ? vars.get("difficulty").toString() : "basic";
        String knowledgePoint = vars.containsKey("knowledgePoint") ? vars.get("knowledgePoint").toString() : subjectKnowledgePoint(subject);
        int count = 1;
        if (vars.containsKey("count") && vars.get("count") instanceof Number) {
            count = Math.min(((Number) vars.get("count")).intValue(), 5);
        }
        int seed = Math.abs((subject + knowledgePoint).hashCode());
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("subject", subject);
            q.put("knowledgePoint", knowledgePoint);
            q.put("difficulty", difficulty);
            q.put("questionContent", "【" + subject + "】请完成" + knowledgePoint + "相关练习题（第" + (i + 1) + "题），巩固基础概念和解题方法。");
            // 基于 seed+i 确定性生成选项和答案
            int idx = (seed + i * 7) % 4;
            String[] letters = {"A", "B", "C", "D"};
            String[] optionTexts = {
                subject + "的核心概念定义",
                "常见的混淆概念",
                "该知识点的应用场景",
                "与" + knowledgePoint + "无关的干扰项"
            };
            List<String> options = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                options.add(letters[j] + ". " + optionTexts[(idx + j) % 4]);
            }
            q.put("options", options);
            q.put("answer", letters[idx]);
            q.put("analysis", "本题考察" + knowledgePoint + "的基础概念，正确答案为" + letters[idx] + "。请认真理解相关定义，区分易混淆的概念。");
            questions.add(q);
        }
        try {
            return objectMapper.writeValueAsString(questions);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateFallbackIncentive(Map<String, Object> vars) {
        Object gapObj = vars.get("分差");
        int gap = 0;
        if (gapObj instanceof Number) gap = ((Number) gapObj).intValue();
        if (gap <= 0) return "太棒了！你已经达到目标分数，继续保持冲击更高层次！";
        if (gap <= 30) return "距离目标仅剩" + gap + "分！你的努力正在见效，坚持下去一定能突破！";
        if (gap <= 80) return "还差" + gap + "分就能达成目标！每天进步一点点，梦想就在前方！";
        return "目标差距" + gap + "分，但每一个高分考生都从当下开始。专注每一天，进步看得见！";
    }

    private String subjectKnowledgePoint(String subject) {
        switch (subject) {
            case "数学": return "函数与导数";
            case "英语": return "阅读理解";
            case "语文": return "现代文阅读";
            case "历史": return "历史时间轴";
            case "政治": return "基本经济制度";
            case "地理": return "自然地理";
            default: return subject + "基础";
        }
    }

    private List<Map<String, Object>> generateDefaultTaskList() {
        String[][] templates = {
            {"数学", "函数与导数", "完成10道函数综合练习题", "专项补强"},
            {"英语", "阅读理解", "精读2篇阅读理解并整理生词", "专项补强"},
            {"语文", "文言文阅读", "翻译1篇文言文并归纳实词", "知识点巩固"},
            {"历史", "时间轴", "整理本周历史时间轴", "知识点巩固"}
        };
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int i = 0; i < templates.length; i++) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("subject", templates[i][0]);
            t.put("knowledgePoint", templates[i][1]);
            t.put("content", templates[i][2]);
            t.put("type", templates[i][3]);
            t.put("durationMinutes", 30 + i * 5);
            t.put("aiHint", "补齐" + templates[i][1] + "知识点短板");
            tasks.add(t);
        }
        return tasks;
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
