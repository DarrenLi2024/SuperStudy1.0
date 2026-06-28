package com.example.ai.generator;

import java.util.List;
import java.util.Map;

/**
 * 题目生成器
 * 基于LLM生成符合考纲、难度适配的习题
 * 当前为占位实现
 */
public interface QuestionGenerator {

    /**
     * 生成题目，返回题目标题、内容、选项、答案、知识点等信息
     */
    Map<String, Object> generateQuestion(String subject, String knowledgePoint, String difficulty);

    /**
     * 批量生成题目
     */
    List<Map<String, Object>> generateBatch(String subject, String difficulty, int count);

    /**
     * 验证题目质量
     */
    boolean validateQuestion(Map<String, Object> question);
}
