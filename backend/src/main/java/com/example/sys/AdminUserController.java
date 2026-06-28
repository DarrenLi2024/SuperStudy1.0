package com.example.sys;

import com.example.dto.ApiPageResult;
import com.example.sys.dto.AdminUserVO;
import com.example.sys.dto.CreateUserRequest;
import com.example.sys.dto.UpdateUserStatusRequest;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseResult<ApiPageResult<AdminUserVO>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseResult.success(userService.listUsers(role, page, size));
    }

    @PostMapping
    public ResponseResult<Map<String, Long>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseResult.success(userService.createUser(request));
    }

    @PutMapping("/{userId}/status")
    public ResponseResult<Void> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateUserStatus(userId, request.getStatus());
        return ResponseResult.success();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseResult<Map<String, String>> resetPassword(@PathVariable Long userId) {
        return ResponseResult.success(userService.resetPassword(userId));
    }
}
