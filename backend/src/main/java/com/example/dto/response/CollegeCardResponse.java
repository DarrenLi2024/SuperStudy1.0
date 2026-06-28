package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 院校卡片数据，用于学生首页三段式展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollegeCardResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CollegeInfo> currentBatchCards;
    private List<CollegeInfo> targetBatchCards;
    private DreamCollegeInfo dreamCollege;
    private String currentBatch;
    private Integer currentScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollegeInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String logo;
        private String batch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DreamCollegeInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String logo;
        private String batch;
        private Integer scoreGap;
        private List<SubjectGap> subjectGaps;
        private String aiIncentive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectGap implements Serializable {
        private static final long serialVersionUID = 1L;
        private String subject;
        private Integer gap;
    }
}
