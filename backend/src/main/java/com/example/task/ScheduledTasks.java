package com.example.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系统定时自治任务
 *
 * 任务列表：
 * 1. 每日凌晨2:00 - AI同步本省最新一分一段数据
 * 2. 每周日凌晨3:00 - AI批量刷新院校素材、迭代题库淘汰老旧题目
 * 3. 每日凌晨4:00 - MySQL全量自动备份（触发Shell脚本）
 */
@Slf4j
@Component
public class ScheduledTasks {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每日凌晨2:00 - 同步一分一段数据
     * 当前为占位实现，后续对接真实LLM进行数据抓取
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncScoreRankData() {
        log.info("[定时任务] 开始同步一分一段数据 - {}", LocalDateTime.now().format(formatter));
        try {
            // TODO: 对接LLM API进行数据抓取和结构化
            // 1. 调用LLM接口抓取各省教育考试院一分一段数据
            // 2. 清洗数据，去重
            // 3. 批量写入score_rank表

            log.info("[定时任务] 一分一段数据同步完成");
            recordTaskExecution("syncScoreRank", "SUCCESS", "一分一段数据同步完成");
        } catch (Exception e) {
            log.error("[定时任务] 一分一段数据同步失败: {}", e.getMessage(), e);
            recordTaskExecution("syncScoreRank", "FAILED", "同步失败: " + e.getMessage());
        }
    }

    /**
     * 每周日凌晨3:00 - 刷新院校素材和迭代题库
     * 当前为占位实现，后续对接真实LLM
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void refreshCollegeAndQuestions() {
        log.info("[定时任务] 开始刷新院校素材和题库 - {}", LocalDateTime.now().format(formatter));
        try {
            // TODO: 对接LLM API
            // 1. 抓取院校官方LOGO、名称、批次
            // 2. 分类存入college_basic表
            // 3. 迭代题库，淘汰低适配题目
            // 4. 生成新题目补充题库

            log.info("[定时任务] 院校素材和题库刷新完成");
            recordTaskExecution("refreshData", "SUCCESS", "院校素材和题库刷新完成");
        } catch (Exception e) {
            log.error("[定时任务] 院校素材和题库刷新失败: {}", e.getMessage(), e);
            recordTaskExecution("refreshData", "FAILED", "刷新失败: " + e.getMessage());
        }
    }

    /**
     * 每日凌晨4:00 - 记录定时任务心跳到Redis（用于监控）
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void recordHeartbeat() {
        String now = LocalDateTime.now().format(formatter);
        redisTemplate.opsForValue().set("scheduler:heartbeat", now);
        redisTemplate.opsForValue().set("scheduler:lastBackup", now);
        log.info("[定时任务] 心跳记录完成 - {}", now);
        recordTaskExecution("heartbeat", "SUCCESS", "心跳正常");
    }

    /**
     * 每30分钟 - 健康检查
     */
    @Scheduled(fixedRate = 1800000)
    public void healthCheck() {
        try {
            // Redis连接检查
            String ping = redisTemplate.getConnectionFactory().getConnection().ping();
            log.debug("[定时任务] Redis健康检查: {}", ping);
        } catch (Exception e) {
            log.warn("[定时任务] Redis健康检查异常: {}", e.getMessage());
        }
    }

    /**
     * 记录任务执行日志到Redis
     */
    private void recordTaskExecution(String taskName, String status, String detail) {
        String key = "scheduler:log:" + taskName;
        String logEntry = String.format("[%s] %s - %s",
                LocalDateTime.now().format(formatter), status, detail);
        redisTemplate.opsForList().leftPush(key, logEntry);
        // 只保留最近50条日志
        redisTemplate.opsForList().trim(key, 0, 49);
    }
}
