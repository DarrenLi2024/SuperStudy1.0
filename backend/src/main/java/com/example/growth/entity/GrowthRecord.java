package com.example.growth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("growth_record")
public class GrowthRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("previous_batch")
    private String previousBatch;

    @TableField("current_batch")
    private String currentBatch;

    @TableField("score_at_upgrade")
    private Integer scoreAtUpgrade;

    @TableField("upgrade_time")
    private Date upgradeTime;

    @TableField("ai_incentive_text")
    private String aiIncentiveText;
}
