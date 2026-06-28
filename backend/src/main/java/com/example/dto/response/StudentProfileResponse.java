package com.example.dto.response;

import com.example.student.entity.StudentProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public static StudentProfileResponse fromEntity(StudentProfile profile) {
        return StudentProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .grade(profile.getGrade())
                .subjectCombination(profile.getSubjectCombination())
                .gaokaoMode(profile.getGaokaoMode())
                .targetScore(profile.getTargetScore())
                .dreamCollege(profile.getDreamCollege())
                .dreamCollegeBatch(profile.getDreamCollegeBatch())
                .baselineScore(profile.getBaselineScore())
                .baselineRank(profile.getBaselineRank())
                .remainingDays(profile.getRemainingDays())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
