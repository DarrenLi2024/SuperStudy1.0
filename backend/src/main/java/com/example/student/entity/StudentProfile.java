package com.example.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String grade;
    private String subjectCombination;
    private String gaokaoMode;
    private Integer targetScore;
    private String dreamCollege;
    private String dreamCollegeBatch;
    private Integer baselineScore;
    private Integer baselineRank;
    private Integer remainingDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
