package com.example.sys;

import com.example.dto.ApiPageResult;
import com.example.sys.dto.*;

import java.util.Map;

public interface UserService {

    LoginResponse login(LoginRequest request);

    UserInfoVO getCurrentUserInfo(Long userId);

    void logout(Long userId);

    ApiPageResult<AdminUserVO> listUsers(String role, Integer page, Integer size);

    Map<String, Long> createUser(CreateUserRequest request);

    void updateUserStatus(Long userId, Integer status);

    Map<String, String> resetPassword(Long userId);
}
