package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("score_rank")
public class ScoreRank implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer year;

    @TableField("subject_type")
    private String subjectType;

    private Integer score;

    @TableField("rank_value")
    private Integer rankValue;

    private String province;
}
