package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.exam.entity.ExamRecord;
import com.example.exam.mapper.ExamRecordMapper;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningAnalysisServiceImpl implements LearningAnalysisService {

    private final ExamRecordMapper examRecordMapper;
    private final ErrorQuestionMapper errorQuestionMapper;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

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

    @Override
    public List<Map<String, Object>> generateDailyTasks(Long studentId) {
        Map<String, Double> weights = calculateSubjectWeights(studentId);
        List<Map<String, Object>> tasks = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, Double> entry : weights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toList())) {
            String subject = entry.getKey();
            String point = weakestKnowledgePoint(studentId, subject);
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", (long) index++);
            task.put("type", entry.getValue() >= 0.35 ? "专项补强" : "知识点巩固");
            task.put("content", subject + "：" + point + "训练 " + minutes(entry.getValue()) + " 分钟");
            task.put("subject", subject);
            task.put("knowledgePoint", point);
            task.put("aiHint", "今日时长权重约" + Math.round(entry.getValue() * 100) + "%，优先补齐该短板。");
            task.put("completionRate", 0.0);
            task.put("status", "pending");
            tasks.add(task);
        }
        return tasks;
    }

    @Override
    public String generateWeeklyReport(Long studentId) {
        return generateReport(studentId, "weekly");
    }

    @Override
    public String generateMonthlyReport(Long studentId) {
        return generateReport(studentId, "monthly");
    }

    @Override
    public Map<String, Object> analyzeKnowledgeMastery(Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : calculateSubjectWeights(studentId).entrySet()) {
            result.put(entry.getKey(), Math.max(20, 100 - Math.round(entry.getValue() * 100)));
        }
        return result;
    }

    @Override
    public String analyzeErrorCause(String subject, String questionContent, String wrongAnswer) {
        LlmRequest request = LlmRequest.builder()
                .taskType("error_analysis")
                .systemPrompt("你是高中错题诊断老师，输出简洁、可执行的错因分析。")
                .userPrompt("学科：" + subject + "\n题目：" + questionContent + "\n错误答案：" + wrongAnswer)
                .temperature(0.2)
                .build();
        try {
            String content = llmClient.generate(request).getContent();
            if (content != null && content.trim().length() > 2 && !"{}".equals(content.trim())) {
                return content.trim();
            }
        } catch (Exception ignored) {
        }
        return "错因集中在" + subject + "基础概念和解题步骤衔接。建议先复盘定义，再做同类题补强。";
    }

    private String generateReport(Long studentId, String type) {
        List<Map<String, Object>> weakSubjects = analyzeWeakSubjects(studentId);
        String period = "weekly".equals(type) ? "周复盘" : "月度成长总结";
        return "【AI" + period + "】本周期主要短板为"
                + weakSubjects.stream().limit(3).map(m -> String.valueOf(m.get("subject"))).collect(Collectors.joining("、"))
                + "。建议下阶段继续按薄弱权重安排任务，并保持错题复盘闭环。";
    }

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
