package com.example.student;

import com.example.security.SecurityUtils;
import com.example.student.dto.CreateProfileRequest;
import com.example.student.dto.StudentProfileVO;
import com.example.student.dto.UpdateProfileRequest;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 获取当前登录学生的档案
     */
    @GetMapping("/profile/me")
    public ResponseResult<StudentProfileVO> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        // 通过userId查询学生档案ID
        StudentProfileVO vo = studentService.getProfile(null, userId, role, null);
        return ResponseResult.success(vo);
    }

    /**
     * 获取学生档案（管理员、家长）
     */
    @GetMapping("/profile/{studentId}")
    public ResponseResult<StudentProfileVO> getProfile(@PathVariable Long studentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        StudentProfileVO vo = studentService.getProfile(studentId, userId, role, null);
        return ResponseResult.success(vo);
    }

    /**
     * 创建学生档案
     */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('student')")
    public ResponseResult<Map<String, Long>> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseResult.success(studentService.createProfile(request, userId));
    }

    /**
     * 更新学生档案
     */
    @PutMapping("/profile/{studentId}")
    public ResponseResult<Void> updateProfile(@PathVariable Long studentId,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        studentService.updateProfile(studentId, request, userId, role);
        return ResponseResult.success();
    }
}
