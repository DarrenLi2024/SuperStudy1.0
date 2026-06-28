package com.example.student;

import com.example.config.BusinessException;
import com.example.student.dto.CreateProfileRequest;
import com.example.student.dto.StudentProfileVO;
import com.example.student.dto.UpdateProfileRequest;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    private StudentProfile createMockProfile(Long id, Long userId, String grade) {
        StudentProfile profile = new StudentProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setGrade(grade);
        profile.setSubjectCombination("物理+化学+生物");
        profile.setGaokaoMode("新高考3+1+2");
        profile.setTargetScore(600);
        profile.setDreamCollege("北京大学");
        profile.setDreamCollegeBatch("双一流");
        return profile;
    }

    @BeforeEach
    void setUp() {
        injectField(studentService, "baseMapper", studentProfileMapper);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Nested
    @DisplayName("创建学生档案测试")
    class CreateProfileTests {

        @Test
        @DisplayName("创建学生档案成功")
        void createProfile_Success() {
            when(studentProfileMapper.selectCount(any())).thenReturn(0L);
            when(studentProfileMapper.insert(any())).thenReturn(1);

            CreateProfileRequest request = new CreateProfileRequest();
            request.setGrade("高二");
            request.setSubjectCombination("历史+政治+地理");
            request.setGaokaoMode("新高考3+1+2");
            request.setTargetScore(600);
            request.setDreamCollege("北京大学");
            request.setDreamCollegeBatch("双一流");

            var result = studentService.createProfile(request, 1L);

            assertNotNull(result);
            assertTrue(result.containsKey("id"));
            verify(studentProfileMapper).insert(any(StudentProfile.class));
        }

        @Test
        @DisplayName("重复创建档案失败")
        void createProfile_Fail_Duplicate() {
            when(studentProfileMapper.selectCount(any())).thenReturn(1L);

            CreateProfileRequest request = new CreateProfileRequest();
            request.setGrade("高二");
            request.setSubjectCombination("历史+政治+地理");
            request.setGaokaoMode("新高考3+1+2");
            request.setTargetScore(600);
            request.setDreamCollege("北京大学");
            request.setDreamCollegeBatch("双一流");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> studentService.createProfile(request, 1L));
            assertTrue(exception.getMessage().contains("已存在"));
        }
    }

    @Nested
    @DisplayName("获取学生档案测试")
    class GetProfileTests {

        @Test
        @DisplayName("获取自己的档案 - 学生")
        void getProfile_Own_Student() {
            StudentProfile profile = createMockProfile(1L, 1L, "高二");
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            StudentProfileVO vo = studentService.getProfile(1L, 1L, "student", null);

            assertNotNull(vo);
            assertEquals("高二", vo.getGrade());
            assertEquals(600, vo.getTargetScore());
        }

        @Test
        @DisplayName("无权访问他人档案")
        void getProfile_OtherStudent_Forbidden() {
            StudentProfile profile = createMockProfile(1L, 2L, "高二");
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> studentService.getProfile(1L, 1L, "student", null));
            assertTrue(exception.getMessage().contains("无权"));
        }
    }

    @Nested
    @DisplayName("更新学生档案测试")
    class UpdateProfileTests {

        @Test
        @DisplayName("更新档案成功")
        void updateProfile_Success() {
            StudentProfile profile = createMockProfile(1L, 1L, "高二");
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setTargetScore(650);
            request.setDreamCollege("清华大学");

            studentService.updateProfile(1L, request, 1L, "student");

            verify(studentProfileMapper).updateById(any());
        }

        @Test
        @DisplayName("家长无权修改")
        void updateProfile_Parent_Forbidden() {
            StudentProfile profile = createMockProfile(1L, 2L, "高二");
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setTargetScore(650);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> studentService.updateProfile(1L, request, 1L, "parent"));
            assertTrue(exception.getMessage().contains("无权"));
        }
    }
}
