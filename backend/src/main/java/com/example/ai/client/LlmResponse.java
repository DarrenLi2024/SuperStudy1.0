package com.example.ai.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** LLM返回的文本内容 */
    private String content;

    /** 解析后的结构化数据（当responseSchema指定时自动解析） */
    @Builder.Default
    private Map<String, Object> structured = new LinkedHashMap<>();

    /** 服务商标识 */
    private String provider;

    /** 模型名 */
    private String model;

    /** 是否命中缓存 */
    private boolean cached;

    /** 是否走降级逻辑 */
    private boolean fallback;

    /** Token使用量 */
    @Builder.Default
    private TokenUsage usage = new TokenUsage();

    /** 耗时（毫秒） */
    private long elapsedMs;

    /**
     * Token使用统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage implements Serializable {
        private static final long serialVersionUID = 1L;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}
