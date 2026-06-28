package com.example.learning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("error_question")
public class ErrorQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    private String subject;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("question_content")
    private String questionContent;

    @TableField("wrong_answer")
    private String wrongAnswer;

    @TableField("ai_analysis")
    private String aiAnalysis;

    @TableField("reinforcement_flag")
    private Integer reinforcementFlag;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;
}
