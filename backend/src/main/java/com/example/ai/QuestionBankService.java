package com.example.ai;

import java.util.List;
import java.util.Map;

/**
 * 智能题库生成服务
 * 根据学生赛道、薄弱知识点实时生成习题
 */
public interface QuestionBankService {

    /**
     * 生成专项训练题目
     */
    List<Map<String, Object>> generateTrainingQuestions(Long studentId, String subject, int count);

    /**
     * 生成补强训练题目（基于错题知识点）
     */
    List<Map<String, Object>> generateReinforcementQuestions(Long studentId, String errorKnowledgePoint, int count);

    /**
     * 根据难度分层生成题目
     */
    List<Map<String, Object>> generateQuestionsByDifficulty(String subject, String difficulty, String knowledgePoint, int count);

    /**
     * 淘汰低适配题目（每周定时任务调用）
     */
    int eliminateLowQualityQuestions();

    /**
     * 补充新题目到题库
     */
    int replenishQuestionBank();
}
