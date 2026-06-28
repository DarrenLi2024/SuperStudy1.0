package com.example.ai;

import java.util.Map;

/**
 * 段位激励文案生成服务
 * 输入当前分、目标分、分差，动态生成激励短句、段位总结
 */
public interface IncentiveService {

    /**
     * 生成段位升级激励文案
     */
    String generateUpgradeIncentive(String previousBatch, String currentBatch, int scoreAtUpgrade);

    /**
     * 生成提分进步激励短句
     */
    String generateProgressIncentive(int currentScore, int previousScore);

    /**
     * 生成心仪院校差距激励
     */
    String generateDreamCollegeIncentive(int currentScore, int targetScore, int gap);

    /**
     * 生成成绩停滞鼓励语
     */
    String generateStagnationEncouragement(int currentScore, int daysSinceImprovement);

    /**
     * 生成每日AI点评
     */
    String generateDailyComment(double completionRate, int taskCount);

    /**
     * 配置文案风格
     */
    void setIncentiveStyle(String style);
}
