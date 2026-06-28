package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * AI生成今日学习任务响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<TaskItem> tasks;
    private String aiComment;
    private Double completionRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String type;
        private String content;
        private String subject;
        private String knowledgePoint;
        private String aiHint;
        private Double completionRate;
        private String status;
    }
}
