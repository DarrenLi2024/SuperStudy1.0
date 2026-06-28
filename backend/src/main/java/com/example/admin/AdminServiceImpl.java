package com.example.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> getMonitorInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // 服务器信息（模拟数据）
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("cpuUsage", 45.2);
        serverInfo.put("memoryUsage", 62.8);
        serverInfo.put("diskUsage", 38.5);
        info.put("serverInfo", serverInfo);

        // 数据库状态
        info.put("databaseStatus", "正常");
        info.put("redisStatus", "正常");

        // 备份状态
        Map<String, Object> backupStatus = new LinkedHashMap<>();
        String lastBackup = (String) redisTemplate.opsForValue().get("scheduler:lastBackup");
        backupStatus.put("lastBackupTime", lastBackup != null ? lastBackup : "暂无备份记录");
        backupStatus.put("backupCount", 0);
        backupStatus.put("status", "正常");
        info.put("backupStatus", backupStatus);

        // AI状态
        Map<String, Object> aiStatus = new LinkedHashMap<>();
        aiStatus.put("modelName", "LLM (待配置)");
        aiStatus.put("apiStatus", "未连接");
        aiStatus.put("lastCallTime", "暂无记录");
        info.put("aiStatus", aiStatus);

        // API统计（模拟数据）
        List<Map<String, Object>> apiStats = new ArrayList<>();
        String[] apis = {"/api/v1/user/login", "/api/v1/exam/submit", "/api/v1/learning/today", "/api/v1/growth/cards"};
        for (String api : apis) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("api", api);
            stat.put("count", new Random().nextInt(100));
            stat.put("avgResponseTime", 50 + new Random().nextInt(200));
            apiStats.add(stat);
        }
        info.put("apiStats", apiStats);

        return info;
    }

    @Override
    public Map<String, Object> getAiConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("low", 500);
        thresholds.put("medium", 580);
        config.put("scoreTrackThresholds", thresholds);

        config.put("safeCollegeRankDiff", 5000);
        config.put("weakSubjectThreshold", 40);
        config.put("incentiveStyle", "正向激励");

        return config;
    }

    @Override
    public void updateAiConfig(Map<String, Object> config) {
        // 保存到Redis，后续由AI服务读取
        redisTemplate.opsForValue().set("ai:config", config);
    }
}
