package com.example.dto.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateStudentProfileRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String grade;
    private String subjectCombination;
    private Integer targetScore;
    private String dreamCollege;
    private String dreamCollegeBatch;
}
