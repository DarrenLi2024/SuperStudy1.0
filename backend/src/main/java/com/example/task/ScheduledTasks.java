package com.example.task;

import com.example.ai.CollegeService;
import com.example.ai.GaokaoDataService;
import com.example.ai.QuestionBankService;
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

    @Autowired(required = false)
    private GaokaoDataService gaokaoDataService;

    @Autowired(required = false)
    private CollegeService collegeService;

    @Autowired(required = false)
    private QuestionBankService questionBankService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每日凌晨2:00 - 同步一分一段数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncScoreRankData() {
        log.info("[定时任务] 开始同步一分一段数据 - {}", LocalDateTime.now().format(formatter));
        try {
            if (gaokaoDataService == null) {
                throw new IllegalStateException("GaokaoDataService未初始化");
            }
            gaokaoDataService.syncScoreRankData();

            log.info("[定时任务] 一分一段数据同步完成");
            recordTaskExecution("syncScoreRank", "SUCCESS", "一分一段数据同步完成");
        } catch (Exception e) {
            log.error("[定时任务] 一分一段数据同步失败: {}", e.getMessage(), e);
            recordTaskExecution("syncScoreRank", "FAILED", "同步失败: " + e.getMessage());
        }
    }

    /**
     * 每周日凌晨3:00 - 刷新院校素材和迭代题库
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void refreshCollegeAndQuestions() {
        log.info("[定时任务] 开始刷新院校素材和题库 - {}", LocalDateTime.now().format(formatter));
        try {
            if (collegeService == null || questionBankService == null) {
                throw new IllegalStateException("院校服务或题库服务未初始化");
            }
            collegeService.refreshColleges();
            int eliminated = questionBankService.eliminateLowQualityQuestions();
            int replenished = questionBankService.replenishQuestionBank();

            log.info("[定时任务] 院校素材和题库刷新完成");
            recordTaskExecution("refreshData", "SUCCESS", "院校素材刷新完成，淘汰题目" + eliminated + "道，补充题目" + replenished + "道");
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
        boolean backupSuccess = runBackupScript();
        redisTemplate.opsForValue().set("scheduler:lastBackup", backupSuccess ? now : "FAILED:" + now);
        log.info("[定时任务] 心跳记录完成 - {}", now);
        recordTaskExecution("heartbeat", backupSuccess ? "SUCCESS" : "FAILED", backupSuccess ? "心跳正常，备份完成" : "心跳正常，备份失败");
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

    private boolean runBackupScript() {
        try {
            java.io.File userDir = new java.io.File(System.getProperty("user.dir"));
            java.io.File projectRoot = new java.io.File(userDir, "scripts").exists() ? userDir : userDir.getParentFile();
            Process process = new ProcessBuilder("bash", "scripts/backup.sh")
                    .directory(projectRoot)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("[定时任务] 自动备份脚本执行失败: {}", e.getMessage());
            return false;
        }
    }
}
