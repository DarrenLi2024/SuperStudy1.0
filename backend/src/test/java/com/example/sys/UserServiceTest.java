package com.example.sys;

import com.example.config.BusinessException;
import com.example.sys.entity.SysUser;
import com.example.sys.mapper.SysUserMapper;
import com.example.util.JwtUtil;
import com.example.util.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenRedisService tokenRedisService;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl();
        // 使用反射注入私有字段
        injectField(userService, "sysUserMapper", sysUserMapper);
        injectField(userService, "passwordEncoder", passwordEncoder);
        injectField(userService, "jwtUtil", jwtUtil);
        injectField(userService, "tokenRedisService", tokenRedisService);
        injectField(userService, "expiration", 86400000L);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }

    private SysUser createMockUser(Long id, String username, String role, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    @Nested
    @DisplayName("登录功能测试")
    class LoginTests {

        @Test
        @DisplayName("成功登录 - 学生用户")
        void login_Success() {
            SysUser user = createMockUser(1L, "student001", "student", 1);
            when(sysUserMapper.selectOne(any())).thenReturn(user);
            when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), anyLong())).thenReturn("mock-token");

            com.example.sys.dto.LoginRequest request = new com.example.sys.dto.LoginRequest();
            request.setUsername("student001");
            request.setPassword("password123");

            com.example.sys.dto.LoginResponse response = userService.login(request);

            assertNotNull(response);
            assertEquals("mock-token", response.getToken());
            assertNotNull(response.getUser());
            assertEquals("student001", response.getUser().getUsername());
            assertEquals("student", response.getUser().getRole());
        }

        @Test
        @DisplayName("登录失败 - 账号不存在")
        void login_Fail_UserNotFound() {
            when(sysUserMapper.selectOne(any())).thenReturn(null);

            com.example.sys.dto.LoginRequest request = new com.example.sys.dto.LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password123");

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(request));
            assertTrue(exception.getMessage().contains("账号或密码错误"));
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_Fail_WrongPassword() {
            SysUser user = createMockUser(1L, "student001", "student", 1);
            when(sysUserMapper.selectOne(any())).thenReturn(user);

            com.example.sys.dto.LoginRequest request = new com.example.sys.dto.LoginRequest();
            request.setUsername("student001");
            request.setPassword("wrongpassword");

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(request));
            assertTrue(exception.getMessage().contains("账号或密码错误"));
        }

        @Test
        @DisplayName("登录失败 - 用户被禁用")
        void login_Fail_UserDisabled() {
            SysUser user = createMockUser(1L, "disabled_user", "student", 0);
            when(sysUserMapper.selectOne(any())).thenReturn(user);

            com.example.sys.dto.LoginRequest request = new com.example.sys.dto.LoginRequest();
            request.setUsername("disabled_user");
            request.setPassword("password123");

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(request));
            assertTrue(exception.getMessage().contains("禁用"));
        }
    }

    @Nested
    @DisplayName("用户管理测试")
    class UserManagementTests {

        @Test
        @DisplayName("创建用户成功")
        void createUser_Success() {
            when(sysUserMapper.selectCount(any())).thenReturn(0L);
            when(sysUserMapper.selectById(anyLong())).thenReturn(createMockUser(2L, "student002", "student", 1));

            com.example.sys.dto.CreateUserRequest request = new com.example.sys.dto.CreateUserRequest();
            request.setUsername("parent001");
            request.setPassword("password123");
            request.setRole("parent");
            request.setStudentId(2L);

            var result = userService.createUser(request);

            assertNotNull(result);
            assertTrue(result.containsKey("id"));
            verify(sysUserMapper).insert(any(SysUser.class));
        }

        @Test
        @DisplayName("创建用户失败 - 用户名已存在")
        void createUser_Fail_DuplicateUsername() {
            when(sysUserMapper.selectCount(any())).thenReturn(1L);

            com.example.sys.dto.CreateUserRequest request = new com.example.sys.dto.CreateUserRequest();
            request.setUsername("existing_user");
            request.setPassword("password123");
            request.setRole("student");

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.createUser(request));
            assertTrue(exception.getMessage().contains("已存在"));
        }

        @Test
        @DisplayName("禁用用户成功")
        void updateUserStatus_Disable_Success() {
            SysUser user = createMockUser(1L, "student001", "student", 1);
            when(sysUserMapper.selectById(1L)).thenReturn(user);

            userService.updateUserStatus(1L, 0);

            verify(sysUserMapper).updateById(any());
            verify(tokenRedisService).removeToken(1L);
        }

        @Test
        @DisplayName("禁用管理员失败")
        void updateUserStatus_Admin_NotAllowed() {
            SysUser admin = createMockUser(1L, "admin", "admin", 1);
            when(sysUserMapper.selectById(1L)).thenReturn(admin);

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUserStatus(1L, 0));
            assertTrue(exception.getMessage().contains("管理员"));
        }
    }

    @Nested
    @DisplayName("查询与信息测试")
    class QueryTests {

        @Test
        @DisplayName("获取当前用户信息成功")
        void getCurrentUserInfo_Success() {
            SysUser user = createMockUser(1L, "student001", "student", 1);
            when(sysUserMapper.selectById(1L)).thenReturn(user);

            var info = userService.getCurrentUserInfo(1L);

            assertNotNull(info);
            assertEquals(1L, info.getId());
            assertEquals("student001", info.getUsername());
            assertEquals("student", info.getRole());
        }

        @Test
        @DisplayName("获取用户列表成功")
        void listUsers_Success() {
            userService.listUsers(null, 1, 10);
            verify(sysUserMapper).selectPage(any(), any());
        }
    }
}
