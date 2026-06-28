package com.example.sys;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.config.BusinessException;
import com.example.dto.ApiPageResult;
import com.example.sys.dto.*;
import com.example.sys.entity.SysUser;
import com.example.sys.mapper.SysUserMapper;
import com.example.util.JwtUtil;
import com.example.util.TokenRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private static final List<String> ALLOWED_ROLES = Arrays.asList("student", "parent", "admin");
    private static final DateTimeFormatter EXPIRE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRedisService tokenRedisService;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getStudentId());
        tokenRedisService.storeToken(user.getId(), token);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpireTime(formatExpireTime());
        response.setUser(toUserInfoVO(user));
        return response;
    }

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserInfoVO(user);
    }

    @Override
    public void logout(Long userId) {
        tokenRedisService.removeToken(userId);
    }

    @Override
    public ApiPageResult<AdminUserVO> listUsers(String role, Integer page, Integer size) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AdminUserVO> list = result.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());

        return new ApiPageResult<>(list, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Long> createUser(CreateUserRequest request) {
        if (!Arrays.asList("student", "parent").contains(request.getRole())) {
            throw new BusinessException(400, "仅可创建学生或家长账号");
        }
        if ("parent".equals(request.getRole()) && request.getStudentId() == null) {
            throw new BusinessException(400, "家长账号必须绑定学生ID");
        }

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "账号已存在");
        }

        if ("parent".equals(request.getRole())) {
            SysUser student = sysUserMapper.selectById(request.getStudentId());
            if (student == null || !"student".equals(student.getRole())) {
                throw new BusinessException(400, "绑定的学生不存在");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStudentId(request.getStudentId());
        user.setStatus(1);
        sysUserMapper.insert(user);

        Map<String, Long> data = new HashMap<>();
        data.put("id", user.getId());
        return data;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值无效");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException(403, "不可禁用管理员账号");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        sysUserMapper.updateById(update);

        if (status == 0) {
            tokenRedisService.removeToken(userId);
        }
    }

    @Override
    public Map<String, String> resetPassword(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException(403, "不可重置管理员密码");
        }

        String newPassword = RandomUtil.randomString(8);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);
        tokenRedisService.removeToken(userId);

        Map<String, String> data = new HashMap<>();
        data.put("newPassword", newPassword);
        return data;
    }

    private UserInfoVO toUserInfoVO(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setStudentId(user.getStudentId());
        vo.setStatus(user.getStatus());
        return vo;
    }

    private AdminUserVO toAdminUserVO(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setStudentId(user.getStudentId());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private String formatExpireTime() {
        Instant expire = Instant.now().plusMillis(expiration);
        return LocalDateTime.ofInstant(expire, ZoneId.systemDefault()).format(EXPIRE_FORMATTER);
    }
}
