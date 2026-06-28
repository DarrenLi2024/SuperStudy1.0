package com.example.student.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String grade;
    private Integer targetScore;
    private String dreamCollege;
    private String dreamCollegeBatch;
}
