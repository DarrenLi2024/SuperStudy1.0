package com.example.admin;

import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取系统监控信息
     */
    @GetMapping("/monitor")
    public ResponseResult<Map<String, Object>> getMonitor() {
        return ResponseResult.success(adminService.getMonitorInfo());
    }

    /**
     * 获取AI参数配置
     */
    @GetMapping("/ai/config")
    public ResponseResult<Map<String, Object>> getAiConfig() {
        return ResponseResult.success(adminService.getAiConfig());
    }

    /**
     * 更新AI参数配置
     */
    @PutMapping("/ai/config")
    public ResponseResult<Void> updateAiConfig(@RequestBody Map<String, Object> config) {
        adminService.updateAiConfig(config);
        return ResponseResult.success();
    }
}
