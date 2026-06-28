package com.example.learning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("task_record")
public class TaskRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("task_date")
    private Date taskDate;

    @TableField("task_content")
    private String taskContent;

    @TableField("completion_rate")
    private java.math.BigDecimal completionRate;

    @TableField("ai_comment")
    private String aiComment;

    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;
}
