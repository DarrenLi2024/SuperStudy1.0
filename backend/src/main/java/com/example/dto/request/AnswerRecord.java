package com.example.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 单题作答记录
 */
@Data
public class AnswerRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    private Long questionId;

    /** 科目 */
    private String subject;

    /** 学生选择的答案 */
    private String selectedAnswer;

    /** 正确答案 */
    private String correctAnswer;

    /** 是否答对 */
    private Boolean isCorrect;
}
