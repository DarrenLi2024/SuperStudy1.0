package com.example.ai.generator;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionGeneratorImpl implements QuestionGenerator {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> generateQuestion(String subject, String knowledgePoint, String difficulty) {
        List<Map<String, Object>> questions = generateBatch(subject, difficulty, 1);
        return questions.isEmpty() ? fallbackQuestion(subject, knowledgePoint, difficulty, 1) : questions.get(0);
    }

    @Override
    public List<Map<String, Object>> generateBatch(String subject, String difficulty, int count) {
        LlmRequest request = LlmRequest.builder()
                .taskType("question_generation")
                .systemPrompt("你是高中全科命题教师，只输出严谨、可验证、适配高考复习的JSON题目。")
                .userPrompt("请生成" + count + "道" + subject + "题，难度为" + difficulty + "。")
                .responseSchema("[{\"subject\":\"数学\",\"knowledgePoint\":\"函数与导数\",\"difficulty\":\"basic\",\"questionContent\":\"题干\",\"options\":[\"A. ...\"],\"answer\":\"A\",\"analysis\":\"解析\"}]")
                .temperature(0.2)
                .build();

        LlmResponse response = llmClient.generate(request);
        if (!response.isFallback()) {
            List<Map<String, Object>> parsed = parseQuestions(response.getContent());
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(fallbackQuestion(subject, defaultKnowledgePoint(subject), difficulty, i + 1));
        }
        return questions;
    }

    @Override
    public boolean validateQuestion(Map<String, Object> question) {
        return question != null
                && notBlank(question.get("subject"))
                && notBlank(question.get("knowledgePoint"))
                && notBlank(question.get("difficulty"))
                && notBlank(question.get("questionContent"))
                && notBlank(question.get("answer"));
    }

    private List<Map<String, Object>> parseQuestions(String content) {
        try {
            return objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            try {
                Map<String, Object> single = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
                return Collections.singletonList(single);
            } catch (Exception ignoredAgain) {
                return Collections.emptyList();
            }
        }
    }

    private Map<String, Object> fallbackQuestion(String subject, String knowledgePoint, String difficulty, int index) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("subject", subject);
        question.put("knowledgePoint", knowledgePoint);
        question.put("difficulty", difficulty);
        question.put("questionContent", buildQuestionContent(subject, knowledgePoint, difficulty, index));
        question.put("options", Arrays.asList("A. 基础概念", "B. 常见误区", "C. 综合应用", "D. 无关结论"));
        question.put("answer", "A");
        question.put("analysis", "本题用于检查" + knowledgePoint + "的核心概念掌握情况，先回到定义，再做推导。");
        return question;
    }

    private String buildQuestionContent(String subject, String knowledgePoint, String difficulty, int index) {
        return "【" + subject + "·" + knowledgePoint + "】第" + index
                + "题：请判断下列关于该知识点的核心说法，选择最符合教材定义的一项。";
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

    private boolean notBlank(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty();
    }
}
