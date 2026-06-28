package com.example.dto.response;

import com.example.exam.entity.ExamRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRecordResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String examType;
    private String subjectScores;
    private Integer totalScore;
    private Integer equivalentGaokaoScore;
    private Integer equivalentRank;
    private String currentBatch;
    private Date examDate;
    private String aiDiagnosisReport;
    private Date createdAt;

    public static ExamRecordResponse fromEntity(ExamRecord record) {
        return ExamRecordResponse.builder()
                .id(record.getId())
                .examType(record.getExamType())
                .subjectScores(record.getSubjectScores())
                .totalScore(record.getTotalScore())
                .equivalentGaokaoScore(record.getEquivalentGaokaoScore())
                .equivalentRank(record.getEquivalentRank())
                .currentBatch(toBatchDisplayName(record.getCurrentBatch()))
                .examDate(record.getExamDate())
                .aiDiagnosisReport(record.getAiDiagnosisReport())
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * 批次编码 → 中文显示名
     */
    private static String toBatchDisplayName(String code) {
        if (code == null) return "未知";
        switch (code) {
            case "985": return "985/双一流";
            case "211": return "211";
            case "first_class": return "普通一本";
            case "second_class": return "公办二本";
            case "below_本科": return "本科以下";
            default: return code;
        }
    }
}
