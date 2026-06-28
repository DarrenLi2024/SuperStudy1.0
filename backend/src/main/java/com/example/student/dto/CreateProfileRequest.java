package com.example.student.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CreateProfileRequest {

    @NotBlank(message = "年级不能为空")
    private String grade;

    @NotBlank(message = "选科组合不能为空")
    private String subjectCombination;

    @NotBlank(message = "高考模式不能为空")
    private String gaokaoMode;

    @NotNull(message = "目标总分不能为空")
    private Integer targetScore;

    @NotBlank(message = "心仪院校不能为空")
    private String dreamCollege;

    @NotBlank(message = "心仪院校批次不能为空")
    private String dreamCollegeBatch;
}
