package com.example.student;

import com.example.student.dto.CreateProfileRequest;
import com.example.student.dto.StudentProfileVO;
import com.example.student.dto.UpdateProfileRequest;

import java.util.Map;

public interface StudentService {

    StudentProfileVO getProfile(Long studentId, Long currentUserId, String currentRole, Long boundStudentId);

    Map<String, Long> createProfile(CreateProfileRequest request, Long userId);

    void updateProfile(Long studentId, UpdateProfileRequest request, Long currentUserId, String currentRole);
}
