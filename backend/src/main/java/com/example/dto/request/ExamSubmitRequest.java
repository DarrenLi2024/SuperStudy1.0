package com.example.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ExamSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "考试类型不能为空")
    private String examType;

    @NotNull(message = "各科分数不能为空")
    private Map<String, Integer> subjectScores;

    @NotBlank(message = "考试日期不能为空")
    private String examDate;

    /**
     * 学生作答记录（可选，用于逐题分析和错题记录）
     * 每项包含: questionId, subject, selectedAnswer, correctAnswer, isCorrect
     */
    private List<AnswerRecord> answers;
}
