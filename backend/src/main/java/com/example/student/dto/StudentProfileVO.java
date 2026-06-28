package com.example.student.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentProfileVO {

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
