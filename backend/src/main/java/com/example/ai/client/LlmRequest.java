package com.example.ai.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务类型标识，用于缓存key和日志追踪 */
    private String taskType;

    /** 系统提示词（角色设定、输出约束） */
    private String systemPrompt;

    /** 用户提示词（具体任务描述） */
    private String userPrompt;

    /** 期望的JSON响应结构（可选，用于约束输出格式） */
    private String responseSchema;

    /** 温度参数，默认0.2（生成类）或0.4（创意类） */
    private Double temperature;

    /** 最大输出token数 */
    @Builder.Default
    private Integer maxTokens = 2048;

    /** 上下文变量 */
    @Builder.Default
    private Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * Few-shot 示例（可选），用于提升输出质量和格式一致性
     * 每条示例包含 user/assistant 两个角色
     */
    @Builder.Default
    private List<FewShotExample> examples = new ArrayList<>();

    /** 是否跳过缓存 */
    @Builder.Default
    private boolean skipCache = false;

    /**
     * Few-shot 示例
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FewShotExample implements Serializable {
        private static final long serialVersionUID = 1L;
        private String user;
        private String assistant;
    }
}
