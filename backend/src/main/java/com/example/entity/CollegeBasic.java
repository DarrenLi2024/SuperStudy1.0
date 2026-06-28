package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("college_basic")
public class CollegeBasic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("college_name")
    private String collegeName;

    @TableField("logo_path")
    private String logoPath;

    @TableField("admission_batch")
    private String admissionBatch;

    @TableField("subject_type")
    private String subjectType;

    @TableField("last_crawled")
    private Date lastCrawled;

    @TableField("min_rank")
    private Integer minRank;

    @TableField("max_rank")
    private Integer maxRank;

    private String province;

    @TableField("`year`")
    private Integer year;
}
