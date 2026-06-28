package com.example.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.config.BusinessException;
import com.example.student.dto.CreateProfileRequest;
import com.example.student.dto.StudentProfileVO;
import com.example.student.dto.UpdateProfileRequest;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentProfileMapper, StudentProfile> implements StudentService {

    @Override
    public StudentProfileVO getProfile(Long studentId, Long currentUserId, String currentRole, Long boundStudentId) {
        StudentProfile profile;

        if (studentId != null) {
            // 按学生ID查询
            profile = this.getById(studentId);
        } else {
            // 按用户ID查询（当前用户自己的档案）
            LambdaQueryWrapper<StudentProfile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(StudentProfile::getUserId, currentUserId);
            profile = this.getOne(queryWrapper);
        }

        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }

        // 权限校验：学生只能查看自己的档案，家长只能查看绑定学生的档案
        if ("student".equals(currentRole) && !profile.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权访问该学生档案");
        }
        if ("parent".equals(currentRole) && boundStudentId != null && !profile.getId().equals(boundStudentId)) {
            throw new BusinessException(403, "无权访问该学生档案");
        }

        return toProfileVO(profile);
    }

    @Override
    public Map<String, Long> createProfile(CreateProfileRequest request, Long userId) {
        // 检查是否已存在档案（仅可创建一次）
        LambdaQueryWrapper<StudentProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentProfile::getUserId, userId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(400, "学生档案已存在，不可重复创建");
        }

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setGrade(request.getGrade());
        profile.setSubjectCombination(request.getSubjectCombination());
        profile.setGaokaoMode(request.getGaokaoMode());
        profile.setTargetScore(request.getTargetScore());
        profile.setDreamCollege(request.getDreamCollege());
        profile.setDreamCollegeBatch(request.getDreamCollegeBatch());
        profile.setRemainingDays(calculateRemainingDays(request.getGrade()));
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        this.save(profile);

        Map<String, Long> result = new HashMap<>();
        result.put("id", profile.getId());
        return result;
    }

    @Override
    public void updateProfile(Long studentId, UpdateProfileRequest request, Long currentUserId, String currentRole) {
        StudentProfile profile = this.getById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }

        // 权限校验：只有学生本人或管理员可更新
        if ("student".equals(currentRole) && !profile.getUserId().equals(currentUserId)) {
            throw new BusinessException(403, "无权修改该学生档案");
        }
        if ("parent".equals(currentRole)) {
            throw new BusinessException(403, "家长无权修改学生档案");
        }

        if (request.getGrade() != null) {
            profile.setGrade(request.getGrade());
            profile.setRemainingDays(calculateRemainingDays(request.getGrade()));
        }
        if (request.getTargetScore() != null) {
            profile.setTargetScore(request.getTargetScore());
        }
        if (request.getDreamCollege() != null) {
            profile.setDreamCollege(request.getDreamCollege());
        }
        if (request.getDreamCollegeBatch() != null) {
            profile.setDreamCollegeBatch(request.getDreamCollegeBatch());
        }

        this.updateById(profile);
    }

    public Integer calculateRemainingDays(String grade) {
        if (grade == null) return null;

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        LocalDate gaokaoDate;

        switch (grade) {
            case "高三":
                gaokaoDate = LocalDate.of(currentYear, 6, 7);
                if (gaokaoDate.isBefore(now)) {
                    gaokaoDate = LocalDate.of(currentYear + 1, 6, 7);
                }
                break;
            case "高二":
                gaokaoDate = LocalDate.of(currentYear + 1, 6, 7);
                break;
            case "高一":
                gaokaoDate = LocalDate.of(currentYear + 2, 6, 7);
                break;
            default:
                return null;
        }

        return (int) ChronoUnit.DAYS.between(now, gaokaoDate);
    }

    // ==================== 私有辅助方法 ====================

    private StudentProfileVO toProfileVO(StudentProfile profile) {
        StudentProfileVO vo = new StudentProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setGrade(profile.getGrade());
        vo.setSubjectCombination(profile.getSubjectCombination());
        vo.setGaokaoMode(profile.getGaokaoMode());
        vo.setTargetScore(profile.getTargetScore());
        vo.setDreamCollege(profile.getDreamCollege());
        vo.setDreamCollegeBatch(profile.getDreamCollegeBatch());
        vo.setBaselineScore(profile.getBaselineScore());
        vo.setBaselineRank(profile.getBaselineRank());
        vo.setRemainingDays(profile.getRemainingDays());
        vo.setCreatedAt(profile.getCreatedAt());
        vo.setUpdatedAt(profile.getUpdatedAt());
        return vo;
    }
}
