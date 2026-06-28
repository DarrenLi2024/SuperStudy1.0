package com.example.sys;

import com.example.security.LoginUser;
import com.example.security.SecurityUtils;
import com.example.sys.dto.LoginRequest;
import com.example.sys.dto.LoginResponse;
import com.example.sys.dto.UserInfoVO;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseResult.success(userService.login(request));
    }

    @GetMapping("/info")
    public ResponseResult<UserInfoVO> info() {
        LoginUser currentUser = SecurityUtils.getCurrentUser();
        return ResponseResult.success(userService.getCurrentUserInfo(currentUser.getUserId()));
    }

    @PostMapping("/logout")
    public ResponseResult<Void> logout() {
        LoginUser currentUser = SecurityUtils.getCurrentUser();
        userService.logout(currentUser.getUserId());
        return ResponseResult.success();
    }
}
