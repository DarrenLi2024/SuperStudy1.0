package com.example.admin;

import com.example.ai.config.LlmProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LlmProperties llmProperties;

    @Override
    public Map<String, Object> getMonitorInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long usableSpace = root.getUsableSpace();
        serverInfo.put("cpuUsage", Math.max(0, Math.round(osBean.getSystemLoadAverage() * 100.0) / 100.0));
        serverInfo.put("memoryUsage", maxMemory <= 0 ? 0 : Math.round((usedMemory * 100.0 / maxMemory) * 10.0) / 10.0);
        serverInfo.put("diskUsage", totalSpace <= 0 ? 0 : Math.round(((totalSpace - usableSpace) * 100.0 / totalSpace) * 10.0) / 10.0);
        info.put("serverInfo", serverInfo);

        info.put("databaseStatus", "应用运行中");
        info.put("redisStatus", redisHealthy() ? "正常" : "异常");

        // 备份状态
        Map<String, Object> backupStatus = new LinkedHashMap<>();
        String lastBackup = (String) redisTemplate.opsForValue().get("scheduler:lastBackup");
        backupStatus.put("lastBackupTime", lastBackup != null ? lastBackup : "暂无备份记录");
        backupStatus.put("backupCount", 0);
        backupStatus.put("status", lastBackup != null && lastBackup.startsWith("FAILED") ? "异常" : "正常");
        info.put("backupStatus", backupStatus);

        Map<String, Object> aiStatus = new LinkedHashMap<>();
        aiStatus.put("modelName", llmProperties.getModel());
        aiStatus.put("provider", llmProperties.getProvider());
        aiStatus.put("apiStatus", llmProperties.isRemoteEnabled() ? "已配置" : "未配置");
        aiStatus.put("lastCallTime", redisTemplate.opsForValue().get("ai:lastCallTime"));
        info.put("aiStatus", aiStatus);

        info.put("taskLogs", readTaskLogs());

        return info;
    }

    @Override
    public Map<String, Object> getAiConfig() {
        Object saved = redisTemplate.opsForValue().get("ai:config");
        if (saved instanceof Map) {
            Map<String, Object> config = new LinkedHashMap<>();
            ((Map<?, ?>) saved).forEach((key, value) -> config.put(String.valueOf(key), value));
            return config;
        }

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
        redisTemplate.opsForValue().set("ai:config", config);
    }

    private boolean redisHealthy() {
        try (RedisConnection connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> readTaskLogs() {
        Map<String, Object> logs = new LinkedHashMap<>();
        for (String task : Arrays.asList("syncScoreRank", "refreshData", "heartbeat")) {
            logs.put(task, redisTemplate.opsForList().range("scheduler:log:" + task, 0, 9));
        }
        return logs;
    }
}
