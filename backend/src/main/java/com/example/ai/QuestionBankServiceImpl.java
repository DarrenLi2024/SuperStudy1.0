package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.generator.QuestionGenerator;
import com.example.entity.AiQuestionBank;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.mapper.AiQuestionBankMapper;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final AiQuestionBankMapper questionBankMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ErrorQuestionMapper errorQuestionMapper;
    private final QuestionGenerator questionGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> generateTrainingQuestions(Long studentId, String subject, int count) {
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        int currentScore = profile != null && profile.getBaselineScore() != null ? profile.getBaselineScore() : 450;
        List<String> difficulties = difficultyPlan(currentScore, count);
        // 按难度分组统计需要多少道，避免同知识点重复
        Map<String, Long> difficultyCounts = difficulties.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (Map.Entry<String, Long> entry : difficultyCounts.entrySet()) {
            // 不限定知识点，从该科目该难度的全量优质题中取
            List<Map<String, Object>> questions = generateQuestionsByDifficultyNoDup(
                    subject, entry.getKey(), entry.getValue().intValue(), seenIds);
            result.addAll(questions);
        }
        return result.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 生成指定数量不重复的题目（跨知识点，避免重复）
     */
    private List<Map<String, Object>> generateQuestionsByDifficultyNoDup(
            String subject, String difficulty, int count, Set<Long> seenIds) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 从题库取已有题（去掉已返回的）
        List<AiQuestionBank> existing = questionBankMapper.selectList(new LambdaQueryWrapper<AiQuestionBank>()
                .eq(AiQuestionBank::getSubject, subject)
                .eq(AiQuestionBank::getDifficulty, difficulty)
                .ge(AiQuestionBank::getQualityScore, BigDecimal.valueOf(0.7))
                .last("LIMIT " + Math.max(1, count * 3)));  // 多取一些用于去重
        Collections.shuffle(existing);

        for (AiQuestionBank q : existing) {
            if (result.size() >= count) break;
            if (seenIds.contains(q.getId())) continue;
            seenIds.add(q.getId());
            result.add(toQuestionMap(q));
        }

        // 2. 不足的用生成补齐
        int missing = count - result.size();
        if (missing <= 0) return result;

        List<Map<String, Object>> generated = questionGenerator.generateBatch(subject, difficulty, missing);
        for (Map<String, Object> question : generated) {
            if (result.size() >= count) break;
            if (questionGenerator.validateQuestion(question)) {
                AiQuestionBank saved = toEntity(question);
                questionBankMapper.insert(saved);
                seenIds.add(saved.getId());
                question.put("id", saved.getId());
                result.add(question);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> generateReinforcementQuestions(Long studentId, String errorKnowledgePoint, int count) {
        String subject = "数学";
        LambdaQueryWrapper<ErrorQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrorQuestion::getStudentId, studentId)
                .eq(ErrorQuestion::getKnowledgePoint, errorKnowledgePoint)
                .last("LIMIT 1");
        ErrorQuestion error = errorQuestionMapper.selectOne(wrapper);
        if (error != null && error.getSubject() != null) {
            subject = error.getSubject();
        }
        return generateQuestionsByDifficulty(subject, "basic", errorKnowledgePoint, count);
    }

    @Override
    public List<Map<String, Object>> generateQuestionsByDifficulty(String subject, String difficulty, String knowledgePoint, int count) {
        List<AiQuestionBank> existing = questionBankMapper.selectList(new LambdaQueryWrapper<AiQuestionBank>()
                .eq(AiQuestionBank::getSubject, subject)
                .eq(AiQuestionBank::getDifficulty, difficulty)
                .eq(knowledgePoint != null && !knowledgePoint.isEmpty(), AiQuestionBank::getKnowledgePoint, knowledgePoint)
                .ge(AiQuestionBank::getQualityScore, BigDecimal.valueOf(0.7))
                .last("LIMIT " + Math.max(1, count)));

        List<Map<String, Object>> result = existing.stream().map(this::toQuestionMap).collect(Collectors.toList());
        int missing = count - result.size();
        if (missing <= 0) {
            return result;
        }

        List<Map<String, Object>> generated = questionGenerator.generateBatch(subject, difficulty, missing);
        for (Map<String, Object> question : generated) {
            if (knowledgePoint != null && !knowledgePoint.isEmpty()) {
                question.put("knowledgePoint", knowledgePoint);
            }
            if (questionGenerator.validateQuestion(question)) {
                AiQuestionBank saved = toEntity(question);
                questionBankMapper.insert(saved);
                question.put("id", saved.getId());
                result.add(question);
            }
        }
        return result;
    }

    @Override
    public int eliminateLowQualityQuestions() {
        List<AiQuestionBank> lowQuality = questionBankMapper.selectList(new LambdaQueryWrapper<AiQuestionBank>()
                .lt(AiQuestionBank::getQualityScore, BigDecimal.valueOf(0.5)));
        int deleted = 0;
        for (AiQuestionBank question : lowQuality) {
            deleted += questionBankMapper.deleteById(question.getId());
        }
        return deleted;
    }

    @Override
    public int replenishQuestionBank() {
        int inserted = 0;
        for (String subject : Arrays.asList("语文", "数学", "英语", "历史", "政治", "地理")) {
            inserted += generateQuestionsByDifficulty(subject, "basic", defaultKnowledgePoint(subject), 2).size();
            inserted += generateQuestionsByDifficulty(subject, "medium", defaultKnowledgePoint(subject), 1).size();
        }
        return inserted;
    }

    private List<String> difficultyPlan(int score, int count) {
        List<String> plan = new ArrayList<>();
        int basic = score <= 500 ? (int) Math.ceil(count * 0.6) : score >= 580 ? Math.max(1, count / 4) : count / 3;
        int hard = score <= 500 ? Math.max(0, count / 10) : score >= 580 ? Math.max(1, count / 3) : count / 3;
        int medium = Math.max(0, count - basic - hard);
        for (int i = 0; i < basic; i++) plan.add("basic");
        for (int i = 0; i < medium; i++) plan.add("medium");
        for (int i = 0; i < hard; i++) plan.add("hard");
        while (plan.size() < count) plan.add("basic");
        return plan;
    }

    private String weakestKnowledgePoint(Long studentId, String subject) {
        List<ErrorQuestion> errors = errorQuestionMapper.selectList(new LambdaQueryWrapper<ErrorQuestion>()
                .eq(ErrorQuestion::getStudentId, studentId)
                .eq(subject != null && !subject.isEmpty(), ErrorQuestion::getSubject, subject));
        return errors.stream()
                .filter(e -> e.getKnowledgePoint() != null && !e.getKnowledgePoint().isEmpty())
                .collect(Collectors.groupingBy(ErrorQuestion::getKnowledgePoint, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultKnowledgePoint(subject));
    }

    private String defaultKnowledgePoint(String subject) {
        if ("数学".equals(subject)) return "函数与导数";
        if ("英语".equals(subject)) return "阅读理解";
        if ("语文".equals(subject)) return "现代文阅读";
        if ("历史".equals(subject)) return "历史时间轴";
        if ("政治".equals(subject)) return "基本经济制度";
        if ("地理".equals(subject)) return "自然地理";
        return subject + "基础";
    }

    private Map<String, Object> toQuestionMap(AiQuestionBank question) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", question.getId());
        map.put("subject", question.getSubject());
        map.put("knowledgePoint", question.getKnowledgePoint());
        map.put("difficulty", question.getDifficulty());
        map.put("questionContent", question.getQuestionContent());
        map.put("options", readOptions(question.getOptionsJson()));
        map.put("answer", question.getAnswer());
        map.put("analysis", question.getAnalysis());
        return map;
    }

    private AiQuestionBank toEntity(Map<String, Object> question) {
        AiQuestionBank entity = new AiQuestionBank();
        entity.setSubject(String.valueOf(question.get("subject")));
        entity.setKnowledgePoint(String.valueOf(question.get("knowledgePoint")));
        entity.setDifficulty(String.valueOf(question.get("difficulty")));
        entity.setQuestionContent(String.valueOf(question.get("questionContent")));
        entity.setOptionsJson(writeOptions(question.get("options")));
        entity.setAnswer(String.valueOf(question.get("answer")));
        entity.setAnalysis(String.valueOf(question.getOrDefault("analysis", "")));
        entity.setScoreRangeTag(String.valueOf(question.getOrDefault("scoreRangeTag", "通用")));
        entity.setQualityScore(BigDecimal.valueOf(0.85));
        entity.setUsageCount(0);
        entity.setSourceType("AI");
        return entity;
    }

    private Object readOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String writeOptions(Object options) {
        try {
            return objectMapper.writeValueAsString(options == null ? Collections.emptyList() : options);
        } catch (Exception e) {
            return "[]";
        }
    }
}
