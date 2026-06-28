package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 成长数据（总分趋势、位次曲线、单科曲线）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthDataResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ScoreTrend> scoreTrend;
    private List<RankTrend> rankTrend;
    private Map<String, List<SubjectTrend>> subjectTrends;
    private String monthlyReport;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrend implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private Integer score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankTrend implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private Integer rank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectTrend implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private Integer score;
    }
}
