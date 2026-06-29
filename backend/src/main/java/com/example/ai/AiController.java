package com.example.ai;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.example.security.StudentAccessService;
import com.example.util.ResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 大模型统一接入端点
 * 
 * 提供同步生成和流式（SSE）生成两种模式，前端 useAI() composable 直接对接此端点。
 * 
 * 端点列表：
 *   POST /api/v1/ai/generate  — 同步生成（请求-响应）
 *   POST /api/v1/ai/stream    — 流式生成（SSE）
 *   GET  /api/v1/ai/status    — AI 服务状态检查
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final LlmClient llmClient;
    private final StudentAccessService studentAccessService;

    /**
     * AI 同步生成端点
     * 
     * 请求体示例：
     * {
     *   "taskType": "daily_tasks",
     *   "systemPrompt": "你是高中学习规划师...",
     *   "userPrompt": "请根据以下数据生成今日任务...",
     *   "temperature": 0.3,
     *   "maxTokens": 2048,
     *   "variables": { "学科权重": "数学:45%, 英语:30%", ... }
     * }
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT','ADMIN')")
    public ResponseResult<Map<String, Object>> generate(@RequestBody Map<String, Object> params) {
        LlmRequest request = buildRequest(params);

        LlmResponse response = llmClient.generate(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", response.getContent());
        result.put("structured", response.getStructured());
        result.put("provider", response.getProvider());
        result.put("model", response.getModel());
        result.put("cached", response.isCached());
        result.put("fallback", response.isFallback());
        result.put("elapsedMs", response.getElapsedMs());
        result.put("usage", response.getUsage());

        return ResponseResult.success(result);
    }

    /**
     * AI 流式生成端点（SSE）
     * 
     * 使用 Server-Sent Events 协议，逐 token 推送到前端。
     * 前端通过 useAI().streamGenerate() 调用。
     * 
     * 请求体与 /generate 相同。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','PARENT','ADMIN')")
    public SseEmitter stream(@RequestBody Map<String, Object> params) {
        LlmRequest request = buildRequest(params);

        // SSE 超时时间 120 秒
        SseEmitter emitter = new SseEmitter(120_000L);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                llmClient.generateStream(request, token -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token));
                    } catch (IOException e) {
                        log.debug("SSE 推送 token 失败: {}", e.getMessage());
                    }
                });

                // 发送完成事件
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"completed\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 流式生成异常: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + e.getMessage() + "\"}"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> executor.shutdownNow());
        emitter.onTimeout(() -> {
            executor.shutdownNow();
            emitter.complete();
        });

        return emitter;
    }

    /**
     * AI 服务状态检查
     * 返回当前 LLM 配置状态，供前端判断 AI 功能是否可用
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('STUDENT','PARENT','ADMIN')")
    public ResponseResult<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        
        // 通过尝试一个简单调用来判断 AI 是否真实可用
        try {
            LlmResponse testResp = llmClient.generate(LlmRequest.builder()
                    .taskType("health_check")
                    .userPrompt("回复 OK")
                    .maxTokens(10)
                    .skipCache(true)
                    .build());
            status.put("available", !testResp.isFallback());
            status.put("provider", testResp.getProvider());
            status.put("model", testResp.getModel());
            status.put("fallback", testResp.isFallback());
        } catch (Exception e) {
            status.put("available", false);
            status.put("provider", "unknown");
            status.put("model", "unknown");
            status.put("error", e.getMessage());
        }

        return ResponseResult.success(status);
    }

    // ==================== 内部方法 ====================

    @SuppressWarnings("unchecked")
    private LlmRequest buildRequest(Map<String, Object> params) {
        LlmRequest.LlmRequestBuilder builder = LlmRequest.builder()
                .taskType(String.valueOf(params.getOrDefault("taskType", "general")))
                .systemPrompt(String.valueOf(params.getOrDefault("systemPrompt", "")))
                .userPrompt(String.valueOf(params.getOrDefault("userPrompt", "")));

        if (params.containsKey("temperature") && params.get("temperature") instanceof Number) {
            builder.temperature(((Number) params.get("temperature")).doubleValue());
        }
        if (params.containsKey("maxTokens") && params.get("maxTokens") instanceof Number) {
            builder.maxTokens(((Number) params.get("maxTokens")).intValue());
        }
        if (params.containsKey("responseSchema")) {
            builder.responseSchema(String.valueOf(params.get("responseSchema")));
        }
        if (params.containsKey("skipCache") && params.get("skipCache") instanceof Boolean) {
            builder.skipCache((Boolean) params.get("skipCache"));
        }
        if (params.containsKey("variables") && params.get("variables") instanceof Map) {
            builder.variables((Map<String, Object>) params.get("variables"));
        }

        return builder.build();
    }
}
