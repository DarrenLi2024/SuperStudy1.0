package com.example.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.config.BusinessException;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentAccessService {

    private final StudentProfileMapper studentProfileMapper;

    public Long requireCurrentStudentId() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (!"student".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可执行该操作");
        }
        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, user.getUserId())
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }
        return profile.getId();
    }

    public void assertReadable(Long studentId) {
        LoginUser user = SecurityUtils.getCurrentUser();
        if ("admin".equals(user.getRole())) {
            return;
        }
        if ("parent".equals(user.getRole())) {
            if (user.getStudentId() != null && user.getStudentId().equals(studentId)) {
                return;
            }
            throw new BusinessException(403, "无权访问该学生数据");
        }
        if ("student".equals(user.getRole())) {
            StudentProfile profile = studentProfileMapper.selectById(studentId);
            if (profile != null && profile.getUserId().equals(user.getUserId())) {
                return;
            }
            throw new BusinessException(403, "无权访问该学生数据");
        }
        throw new BusinessException(403, "无权访问该学生数据");
    }

    public void assertWritable(Long studentId) {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (!"student".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生本人可修改学习数据");
        }
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }
        if (!profile.getUserId().equals(user.getUserId())) {
            throw new BusinessException(403, "无权修改该学生数据");
        }
    }
}
