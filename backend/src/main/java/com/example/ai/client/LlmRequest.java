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
public class LlmRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskType;
    private String systemPrompt;
    private String userPrompt;
    private String responseSchema;
    private Double temperature;

    @Builder.Default
    private Map<String, Object> variables = new LinkedHashMap<>();
}
