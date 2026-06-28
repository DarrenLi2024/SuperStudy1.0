package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 段位进度条数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthProgressResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Phase> phases;
    private Double totalProgress;
    private Integer targetScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Phase implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private Integer startScore;
        private Integer endScore;
        private Integer currentScore;
        private Double progress;
        private Boolean completed;
    }
}
