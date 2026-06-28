package com.example.ai;

import java.util.List;
import java.util.Map;

/**
 * 学情分析服务
 * 输入历次分数、错题集合，输出薄弱标签、时长权重、任务清单
 */
public interface LearningAnalysisService {

    /**
     * 分析薄弱学科
     */
    List<Map<String, Object>> analyzeWeakSubjects(Long studentId);

    /**
     * 计算各科学科权重（极弱分配40%时长）
     */
    Map<String, Double> calculateSubjectWeights(Long studentId);

    /**
     * 生成个性化每日任务
     */
    List<Map<String, Object>> generateDailyTasks(Long studentId);

    /**
     * 生成周复盘报告
     */
    String generateWeeklyReport(Long studentId);

    /**
     * 生成月度成长总结
     */
    String generateMonthlyReport(Long studentId);

    /**
     * 分析知识点掌握度
     */
    Map<String, Object> analyzeKnowledgeMastery(Long studentId);

    /**
     * 生成AI错因分析
     */
    String analyzeErrorCause(String subject, String questionContent, String wrongAnswer);
}
