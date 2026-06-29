package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.example.exam.entity.ExamRecord;
import com.example.exam.mapper.ExamRecordMapper;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningAnalysisServiceImpl implements LearningAnalysisService {

    private final ExamRecordMapper examRecordMapper;
    private final ErrorQuestionMapper errorQuestionMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    // ==================== 薄弱学科分析 ====================

    @Override
    public List<Map<String, Object>> analyzeWeakSubjects(Long studentId) {
        Map<String, Double> weights = calculateSubjectWeights(studentId);
        return weights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("subject", entry.getKey());
                    item.put("weight", entry.getValue());
                    item.put("level", entry.getValue() >= 0.35 ? "weak" : entry.getValue() >= 0.2 ? "normal" : "stable");
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> calculateSubjectWeights(Long studentId) {
        Map<String, Integer> latestScores = latestSubjectScores(studentId);
        Map<String, Long> errorCounts = errorQuestionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                        .eq(ErrorQuestion::getStudentId, studentId))
                .stream()
                .filter(e -> e.getSubject() != null)
                .collect(Collectors.groupingBy(ErrorQuestion::getSubject, Collectors.counting()));

        List<String> subjects = new ArrayList<>(Arrays.asList("语文", "数学", "英语", "历史", "政治", "地理"));
        Map<String, Double> raw = new LinkedHashMap<>();
        for (String subject : subjects) {
            int score = latestScores.getOrDefault(subject, defaultScore(subject));
            long errors = errorCounts.getOrDefault(subject, 0L);
            double weakness = Math.max(0, targetScore(subject) - score) + errors * 8.0;
            raw.put(subject, Math.max(5.0, weakness));
        }

        String weakest = raw.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("数学");
        double total = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            weights.put(entry.getKey(), round(entry.getValue() / total));
        }
        if (weights.getOrDefault(weakest, 0.0) < 0.4 && raw.get(weakest) >= 40) {
            double remaining = 0.6;
            double others = weights.entrySet().stream()
                    .filter(e -> !weakest.equals(e.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            weights.put(weakest, 0.4);
            for (String subject : subjects) {
                if (!weakest.equals(subject)) {
                    weights.put(subject, round(weights.get(subject) / others * remaining));
                }
            }
        }
        return weights;
    }

    // ==================== 每日任务生成（LLM驱动） ====================

    @Override
    public List<Map<String, Object>> generateDailyTasks(Long studentId) {
        Map<String, Double> weights = calculateSubjectWeights(studentId);
        Map<String, Integer> scores = latestSubjectScores(studentId);
        Map<String, String> weakPoints = new LinkedHashMap<>();
        for (String subject : weights.keySet()) {
            weakPoints.put(subject, weakestKnowledgePoint(studentId, subject));
        }

        // 获取学生信息
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        String grade = profile != null && profile.getGrade() != null ? profile.getGrade() : "高三";
        int baselineScore = profile != null && profile.getBaselineScore() != null ? profile.getBaselineScore() : 450;

        // 构建上下文
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("年级", grade);
        variables.put("基线分", baselineScore);
        variables.put("学科权重", weights.entrySet().stream()
                .map(e -> e.getKey() + ":" + Math.round(e.getValue() * 100) + "%")
                .collect(Collectors.joining(", ")));
        variables.put("各科薄弱知识点", weakPoints);
        variables.put("近期分数", scores);

        // 尝试LLM生成
        try {
            LlmResponse response = llmClient.generate(LlmRequest.builder()
                    .taskType("daily_tasks")
                    .systemPrompt(buildTaskSystemPrompt())
                    .userPrompt("请根据以下学生数据，生成今日个性化学习任务（4-5项）。")
                    .responseSchema(buildTaskSchema())
                    .temperature(0.3)
                    .variables(variables)
                    .build());

            if (!response.isFallback() && response.getStructured() != null
                    && !response.getStructured().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) response.getStructured()
                        .getOrDefault("items", Collections.emptyList());
                if (tasks != null && !tasks.isEmpty()) {
                    return enrichTasks(tasks, weights, weakPoints);
                }
            }
        } catch (Exception e) {
            log.debug("LLM任务生成失败，使用规则生成: {}", e.getMessage());
        }

        // 降级：规则生成
        return ruleBasedTasks(weights, studentId, weakPoints);
    }

    // ==================== 周报/月报（LLM驱动） ====================

    @Override
    public String generateWeeklyReport(Long studentId) {
        return generateAiReport(studentId, "weekly");
    }

    @Override
    public String generateMonthlyReport(Long studentId) {
        return generateAiReport(studentId, "monthly");
    }

    private String generateAiReport(Long studentId, String type) {
        List<Map<String, Object>> weakSubjects = analyzeWeakSubjects(studentId);
        Map<String, Integer> scores = latestSubjectScores(studentId);
        Map<String, Long> errorCounts = errorQuestionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                        .eq(ErrorQuestion::getStudentId, studentId))
                .stream()
                .filter(e -> e.getSubject() != null)
                .collect(Collectors.groupingBy(ErrorQuestion::getSubject, Collectors.counting()));

        String period = "weekly".equals(type) ? "本周" : "本月";
        String periodLabel = "weekly".equals(type) ? "周复盘" : "月度成长总结";

        // 构建上下文
        StringBuilder context = new StringBuilder();
        context.append("## 学生" + period + "学习数据\n\n");
        context.append("### 学科分数\n");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            context.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("分\n");
        }
        context.append("\n### 薄弱学科（权重从高到低）\n");
        for (Map<String, Object> ws : weakSubjects) {
            context.append("- ").append(ws.get("subject")).append(": ")
                    .append(ws.get("level")).append(" (权重")
                    .append(Math.round((Double) ws.get("weight") * 100)).append("%)\n");
        }
        context.append("\n### 错题统计\n");
        for (Map.Entry<String, Long> entry : errorCounts.entrySet()) {
            context.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("道\n");
        }

        try {
            LlmResponse response = llmClient.generate(LlmRequest.builder()
                    .taskType(type + "_report")
                    .systemPrompt(buildReportSystemPrompt(periodLabel))
                    .userPrompt(context.toString()
                            + "\n请生成一份" + periodLabel + "，包含：学习概况、薄弱点分析、下阶段建议。300字以内。")
                    .temperature(0.4)
                    .maxTokens(1024)
                    .build());

            if (!response.isFallback() && response.getContent() != null
                    && response.getContent().length() > 10 && !"{}".equals(response.getContent())) {
                return "【AI" + periodLabel + "】\n" + response.getContent().trim();
            }
        } catch (Exception e) {
            log.debug("LLM报告生成失败: {}", e.getMessage());
        }

        // 降级
        return "【AI" + periodLabel + "】本周期主要短板为"
                + weakSubjects.stream().limit(3).map(m -> String.valueOf(m.get("subject"))).collect(Collectors.joining("、"))
                + "。建议下阶段继续按薄弱权重安排任务，并保持错题复盘闭环。";
    }

    // ==================== 知识点掌握度（LLM驱动） ====================

    @Override
    public Map<String, Object> analyzeKnowledgeMastery(Long studentId) {
        Map<String, Double> weights = calculateSubjectWeights(studentId);
        Map<String, Object> result = new LinkedHashMap<>();

        // 尝试LLM深度分析
        try {
            Map<String, Integer> scores = latestSubjectScores(studentId);
            Map<String, Long> errorCounts = errorQuestionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                            .eq(ErrorQuestion::getStudentId, studentId))
                    .stream()
                    .filter(e -> e.getSubject() != null)
                    .collect(Collectors.groupingBy(ErrorQuestion::getSubject, Collectors.counting()));

            StringBuilder context = new StringBuilder("## 学生学情数据\n");
            for (String subject : weights.keySet()) {
                context.append("- ").append(subject).append(": 分数")
                        .append(scores.getOrDefault(subject, 0)).append(", 错题")
                        .append(errorCounts.getOrDefault(subject, 0L)).append("道\n");
            }

            LlmResponse response = llmClient.generate(LlmRequest.builder()
                    .taskType("knowledge_mastery")
                    .systemPrompt("你是高中教学诊断专家。根据学生各科分数和错题数据，评估每科的知识点掌握度(0-100%)，并给出简短建议。")
                    .userPrompt(context.toString() + "\n请输出JSON，key为学科名，value为对象{mastery: 掌握度百分比, advice: 建议}")
                    .responseSchema("{\"数学\": {\"mastery\": 65, \"advice\": \"加强函数与导数练习\"}}")
                    .temperature(0.2)
                    .build());

            if (!response.isFallback() && response.getStructured() != null
                    && !response.getStructured().isEmpty()) {
                return response.getStructured();
            }
        } catch (Exception e) {
            log.debug("LLM知识点分析失败: {}", e.getMessage());
        }

        // 降级：基于权重的简单计算
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            Map<String, Object> subjectResult = new LinkedHashMap<>();
            subjectResult.put("mastery", Math.max(20, 100 - Math.round(entry.getValue() * 100)));
            subjectResult.put("advice", "建议加强" + entry.getKey() + "基础训练");
            result.put(entry.getKey(), subjectResult);
        }
        return result;
    }

    // ==================== 错因分析（LLM驱动） ====================

    @Override
    public String analyzeErrorCause(String subject, String questionContent, String wrongAnswer) {
        LlmRequest request = LlmRequest.builder()
                .taskType("error_analysis")
                .systemPrompt(buildErrorAnalysisPrompt())
                .userPrompt("学科：" + subject + "\n题目内容：" + questionContent + "\n学生错误答案：" + wrongAnswer)
                .temperature(0.2)
                .maxTokens(512)
                .build();

        try {
            LlmResponse response = llmClient.generate(request);
            String content = response.getContent();
            if (content != null && content.trim().length() > 2 && !"{}".equals(content.trim())) {
                return content.trim();
            }
        } catch (Exception e) {
            log.debug("LLM错因分析失败: {}", e.getMessage());
        }
        return "错因集中在" + subject + "基础概念和解题步骤衔接。建议先复盘定义，再做同类题补强。";
    }

    // ==================== Prompt模板 ====================

    private String buildTaskSystemPrompt() {
        return """
                你是高中学习规划师，专门为高三学生制定每日个性化学习任务。

                ## 任务设计原则
                1. 优先分配时间给薄弱学科（权重高的学科）
                2. 每个任务需指定：学科、知识点、建议时长、任务类型
                3. 任务类型包括：专项补强（权重≥35%的学科）、知识点巩固（权重20-35%）、常规练习（权重<20%）
                4. 每日总时长控制在120-180分钟
                5. 任务要具体可执行，给出明确的练习方向

                ## 输出要求
                输出JSON数组，每条任务包含：subject, knowledgePoint, type, content, durationMinutes, aiHint

                ## 安全约束
                - 不得承诺具体提分幅度
                - 不得推荐未经教育部门认证的学习方法
                - 语言为简体中文
                """;
    }

    private String buildTaskSchema() {
        return """
                [
                  {
                    "subject": "数学",
                    "knowledgePoint": "函数与导数",
                    "type": "专项补强",
                    "content": "完成函数单调性判定练习10题，重点掌握导数法判断增减区间",
                    "durationMinutes": 45,
                    "aiHint": "函数题占高考数学约25%，是提分性价比最高的模块"
                  }
                ]""";
    }

    private String buildReportSystemPrompt(String periodLabel) {
        return """
                你是高中升学陪伴系统的学情分析师，专门撰写学习%s。

                ## 撰写要求
                1. 先肯定学生的努力和进步
                2. 客观分析薄弱环节
                3. 给出具体可执行的下阶段建议
                4. 语气温暖、鼓励但不浮夸
                5. 控制在300字以内

                ## 安全约束
                - 不得承诺具体提分数字
                - 不得制造焦虑
                - 语言为简体中文
                """.formatted(periodLabel);
    }

    private String buildErrorAnalysisPrompt() {
        return """
                你是高中错题诊断专家。请分析学生的错误原因，输出简洁、可执行的改进建议。

                ## 分析维度
                1. 概念理解错误（基础知识不牢）
                2. 解题步骤遗漏（跳步、漏条件）
                3. 计算失误（粗心、运算错误）
                4. 题型陌生（没见过类似题）

                ## 输出要求
                - 先指出错误类型
                - 再给出具体改进方法（1-2句话）
                - 总字数控制在80字以内

                ## 安全约束
                - 语言为简体中文
                - 不贬低学生
                """;
    }

    // ==================== 降级方法 ====================

    private List<Map<String, Object>> ruleBasedTasks(Map<String, Double> weights, Long studentId,
                                                      Map<String, String> weakPoints) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, Double> entry : weights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toList())) {
            String subject = entry.getKey();
            String point = weakPoints.getOrDefault(subject, defaultKnowledgePoint(subject));
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", (long) index++);
            task.put("type", entry.getValue() >= 0.35 ? "专项补强" : "知识点巩固");
            task.put("content", subject + "：" + point + "训练 " + minutes(entry.getValue()) + " 分钟");
            task.put("subject", subject);
            task.put("knowledgePoint", point);
            task.put("durationMinutes", minutes(entry.getValue()));
            task.put("aiHint", "今日时长权重约" + Math.round(entry.getValue() * 100) + "%，优先补齐该短板。");
            task.put("completionRate", 0.0);
            task.put("status", "pending");
            tasks.add(task);
        }
        return tasks;
    }

    private List<Map<String, Object>> enrichTasks(List<Map<String, Object>> llmTasks,
                                                   Map<String, Double> weights,
                                                   Map<String, String> weakPoints) {
        int index = 1;
        for (Map<String, Object> task : llmTasks) {
            task.put("id", (long) index++);
            task.putIfAbsent("completionRate", 0.0);
            task.putIfAbsent("status", "pending");
            String subject = String.valueOf(task.getOrDefault("subject", ""));
            task.putIfAbsent("knowledgePoint", weakPoints.getOrDefault(subject, subject + "基础"));
            task.putIfAbsent("durationMinutes", minutes(weights.getOrDefault(subject, 0.2)));
        }
        return llmTasks;
    }

    // ==================== 数据辅助方法 ====================

    private Map<String, Integer> latestSubjectScores(Long studentId) {
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getExamDate)
                .last("LIMIT 1"));
        if (record == null || record.getSubjectScores() == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(record.getSubjectScores(), new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String weakestKnowledgePoint(Long studentId, String subject) {
        return errorQuestionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                        .eq(ErrorQuestion::getStudentId, studentId)
                        .eq(ErrorQuestion::getSubject, subject))
                .stream()
                .filter(e -> e.getKnowledgePoint() != null)
                .collect(Collectors.groupingBy(ErrorQuestion::getKnowledgePoint, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultKnowledgePoint(subject));
    }

    private int defaultScore(String subject) {
        return "数学".equals(subject) ? 50 : 70;
    }

    private int targetScore(String subject) {
        return Arrays.asList("语文", "数学", "英语").contains(subject) ? 110 : 75;
    }

    private int minutes(double weight) {
        return Math.max(20, (int) Math.round(weight * 120));
    }

    private String defaultKnowledgePoint(String subject) {
        if ("数学".equals(subject)) return "函数与导数";
        if ("英语".equals(subject)) return "阅读理解";
        if ("语文".equals(subject)) return "现代文阅读";
        return subject + "基础";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
