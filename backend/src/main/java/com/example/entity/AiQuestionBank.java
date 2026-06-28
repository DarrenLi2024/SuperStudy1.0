package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("ai_question_bank")
public class AiQuestionBank implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String subject;

    @TableField("knowledge_point")
    private String knowledgePoint;

    private String difficulty;

    @TableField("question_content")
    private String questionContent;

    @TableField("options_json")
    private String optionsJson;

    private String answer;

    private String analysis;

    @TableField("score_range_tag")
    private String scoreRangeTag;

    @TableField("quality_score")
    private BigDecimal qualityScore;

    @TableField("usage_count")
    private Integer usageCount;

    @TableField("source_type")
    private String sourceType;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
