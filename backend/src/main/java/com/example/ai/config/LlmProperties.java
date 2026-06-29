package com.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    private String provider = "local";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "local-deterministic";
    private Integer timeoutMs = 5000;
    private Integer maxRetries = 1;
    private Boolean cacheEnabled = true;
    private Long cacheTtlSeconds = 1800L;
    /** 是否启用流式输出（SSE） */
    private Boolean streamEnabled = true;

    public boolean isRemoteEnabled() {
        return baseUrl != null && !baseUrl.trim().isEmpty()
                && apiKey != null && !apiKey.trim().isEmpty()
                && !"local".equalsIgnoreCase(provider);
    }
}
