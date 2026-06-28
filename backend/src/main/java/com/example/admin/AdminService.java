package com.example.admin;

import java.util.Map;

public interface AdminService {

    /**
     * 获取系统监控信息
     */
    Map<String, Object> getMonitorInfo();

    /**
     * 获取AI参数配置
     */
    Map<String, Object> getAiConfig();

    /**
     * 更新AI参数配置
     */
    void updateAiConfig(Map<String, Object> config);
}
