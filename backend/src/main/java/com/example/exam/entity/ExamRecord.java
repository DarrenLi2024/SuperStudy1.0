package com.example.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("exam_type")
    private String examType;

    @TableField("subject_scores")
    private String subjectScores;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("equivalent_gaokao_score")
    private Integer equivalentGaokaoScore;

    @TableField("equivalent_rank")
    private Integer equivalentRank;

    @TableField("current_batch")
    private String currentBatch;

    @TableField("exam_date")
    private Date examDate;

    @TableField("ai_diagnosis_report")
    private String aiDiagnosisReport;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;
}
