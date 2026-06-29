package com.example.ai.generator;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGeneratorImpl implements QuestionGenerator {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    /** 各科知识点映射 */
    private static final Map<String, List<String>> KNOWLEDGE_POINTS = new LinkedHashMap<>();
    static {
        KNOWLEDGE_POINTS.put("语文", Arrays.asList(
                "现代文阅读", "文言文阅读", "古诗词鉴赏", "语言文字运用", "写作-议论文", "写作-记叙文"));
        KNOWLEDGE_POINTS.put("数学", Arrays.asList(
                "函数与导数", "三角函数与解三角形", "数列", "立体几何", "解析几何",
                "概率与统计", "向量", "不等式"));
        KNOWLEDGE_POINTS.put("英语", Arrays.asList(
                "阅读理解", "完形填空", "语法填空", "短文改错", "书面表达", "听力理解"));
        KNOWLEDGE_POINTS.put("物理", Arrays.asList(
                "力学", "电磁学", "热学", "光学", "原子物理", "实验"));
        KNOWLEDGE_POINTS.put("化学", Arrays.asList(
                "化学基本概念", "元素化合物", "有机化学", "化学反应原理", "物质结构", "化学实验"));
        KNOWLEDGE_POINTS.put("生物", Arrays.asList(
                "细胞生物学", "遗传与进化", "稳态与环境", "生物技术", "实验探究"));
        KNOWLEDGE_POINTS.put("历史", Arrays.asList(
                "中国古代史", "中国近现代史", "世界古代史", "世界近现代史", "历史时间轴", "史料分析"));
        KNOWLEDGE_POINTS.put("政治", Arrays.asList(
                "经济生活", "政治生活", "文化生活", "生活与哲学", "时事政治"));
        KNOWLEDGE_POINTS.put("地理", Arrays.asList(
                "自然地理", "人文地理", "区域地理", "地理信息技术", "环境与可持续发展"));
    }

    /** 各科题型 */
    private static final Map<String, List<String>> QUESTION_TYPES = new LinkedHashMap<>();
    static {
        QUESTION_TYPES.put("语文", Arrays.asList("阅读理解", "文言文翻译", "诗歌鉴赏", "语言运用", "作文"));
        QUESTION_TYPES.put("数学", Arrays.asList("选择题", "填空题", "解答题"));
        QUESTION_TYPES.put("英语", Arrays.asList("阅读理解", "完形填空", "语法填空", "写作"));
        QUESTION_TYPES.put("物理", Arrays.asList("选择题", "实验题", "计算题"));
        QUESTION_TYPES.put("化学", Arrays.asList("选择题", "填空题", "推断题", "计算题"));
        QUESTION_TYPES.put("生物", Arrays.asList("选择题", "填空题", "简答题"));
        QUESTION_TYPES.put("历史", Arrays.asList("选择题", "材料解析题", "论述题"));
        QUESTION_TYPES.put("政治", Arrays.asList("选择题", "简答题", "论述题"));
        QUESTION_TYPES.put("地理", Arrays.asList("选择题", "综合题"));
    }

    @Override
    public Map<String, Object> generateQuestion(String subject, String knowledgePoint, String difficulty) {
        List<Map<String, Object>> questions = generateBatch(subject, difficulty, 1);
        return questions.isEmpty() ? fallbackQuestion(subject, knowledgePoint, difficulty, 1) : questions.get(0);
    }

    @Override
    public List<Map<String, Object>> generateBatch(String subject, String difficulty, int count) {
        // 选择合适知识点
        List<String> points = KNOWLEDGE_POINTS.getOrDefault(subject,
                Collections.singletonList(subject + "基础"));
        String knowledgePoint = points.get(new Random().nextInt(points.size()));

        // 构建增强的Prompt
        LlmRequest request = LlmRequest.builder()
                .taskType("question_generation")
                .systemPrompt(buildSystemPrompt(subject, difficulty))
                .userPrompt(buildUserPrompt(subject, knowledgePoint, difficulty, count))
                .responseSchema(buildSchema(subject))
                .temperature(difficultyLevel(difficulty) >= 2 ? 0.3 : 0.2)
                .maxTokens(4096)
                .build();

        LlmResponse response = llmClient.generate(request);

        if (!response.isFallback()) {
            List<Map<String, Object>> parsed = parseQuestions(response.getContent());
            if (!parsed.isEmpty()) {
                // 补充默认字段
                for (Map<String, Object> q : parsed) {
                    q.putIfAbsent("subject", subject);
                    q.putIfAbsent("knowledgePoint", knowledgePoint);
                    q.putIfAbsent("difficulty", difficulty);
                    q.putIfAbsent("scoreRangeTag", scoreRangeTag(difficulty));
                }
                log.info("LLM出题成功: {} {}难度 {}道", subject, difficulty, parsed.size());
                return parsed;
            }
        }

        // 降级：生成模板题目
        log.debug("LLM出题降级，使用模板生成: {} {}难度 {}道", subject, difficulty, count);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String point = points.get(i % points.size());
            questions.add(fallbackQuestion(subject, point, difficulty, i + 1));
        }
        return questions;
    }

    @Override
    public boolean validateQuestion(Map<String, Object> question) {
        if (question == null) return false;
        // 基础字段验证
        if (!notBlank(question.get("subject"))) return false;
        if (!notBlank(question.get("questionContent"))) return false;
        if (!notBlank(question.get("answer"))) return false;

        // 选择题必须有选项
        String subject = String.valueOf(question.get("subject"));
        if (isChoiceSubject(subject)) {
            Object options = question.get("options");
            if (options instanceof List && ((List<?>) options).size() >= 2) {
                return true;
            }
            if (options instanceof String) {
                return !((String) options).trim().isEmpty();
            }
            return false;
        }
        return true;
    }

    // ==================== Prompt构建 ====================

    private String buildSystemPrompt(String subject, String difficulty) {
        String typeHint = isChoiceSubject(subject) ? "包含4个选项的选择题" : "简答/填空题";
        String difficultyHint = switch (difficulty) {
            case "basic" -> "基础概念题，考查核心知识点记忆和基本理解";
            case "medium" -> "中等难度题，考查知识综合运用和迁移能力";
            case "hard" -> "高难度题，考查深度分析和创新解题能力";
            default -> "适配高考难度的题目";
        };

        return String.format("""
                你是中国高考%s命题专家，具有10年以上命题经验。

                ## 命题要求
                - 题型：%s
                - 难度：%s（%s）
                - 题目必须严谨、科学、可验证
                - 所有数值和公式必须准确无误
                - 解析要详细，说明解题思路和关键步骤
                - 贴合最新高考大纲和命题趋势

                ## 输出要求
                - 严格按JSON格式输出
                - 选择题的options为字符串数组
                - answer为正确答案（选择题填选项字母，解答题填关键步骤）

                ## 安全约束
                - 题目内容必须符合中国教育方针
                - 不得涉及政治敏感、暴力、色情等内容
                - 语言为简体中文
                """, subject, typeHint, difficulty, difficultyHint);
    }

    private String buildUserPrompt(String subject, String knowledgePoint, String difficulty, int count) {
        return String.format("""
                请生成%d道%s题，知识点为"%s"，难度为"%s"。

                要求：
                1. 题目之间不要重复
                2. 覆盖该知识点的不同考查角度
                3. 题目表述清晰，无歧义
                4. 每道题附详细解析
                """, count, subject, knowledgePoint, difficulty);
    }

    private String buildSchema(String subject) {
        if (isChoiceSubject(subject)) {
            return """
                    [
                      {
                        "subject": "数学",
                        "knowledgePoint": "函数与导数",
                        "difficulty": "medium",
                        "questionContent": "已知函数f(x)=x²-4x+3，则f(x)在区间[1,3]上的最小值为？",
                        "options": ["A. -1", "B. 0", "C. -2", "D. 1"],
                        "answer": "A",
                        "analysis": "f(x)=(x-2)²-1，对称轴x=2在[1,3]内，最小值为f(2)=-1"
                      }
                    ]""";
        }
        return """
                [
                  {
                    "subject": "语文",
                    "knowledgePoint": "现代文阅读",
                    "difficulty": "medium",
                    "questionContent": "阅读以下文段，分析作者使用了哪些修辞手法及其表达效果。（文段内容）",
                    "options": [],
                    "answer": "作者使用了比喻和排比的修辞手法。比喻将...比作...，生动形象地表现了...；排比增强了语势，强调了...",
                    "analysis": "本题考查修辞手法的识别与赏析。首先找出文中的修辞句，然后分析其表达效果..."
                  }
                ]""";
    }

    // ==================== 解析 ====================

    private List<Map<String, Object>> parseQuestions(String content) {
        if (content == null || content.trim().isEmpty() || "{}".equals(content.trim())) {
            return Collections.emptyList();
        }

        // 清理markdown代码块
        String cleaned = content.trim();
        Matcher matcher = JSON_BLOCK.matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group(1).trim();
        }

        try {
            // 尝试数组
            if (cleaned.startsWith("[")) {
                return objectMapper.readValue(cleaned, new TypeReference<List<Map<String, Object>>>() {});
            }
            // 尝试对象
            if (cleaned.startsWith("{")) {
                Map<String, Object> wrapper = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) wrapper.getOrDefault("questions",
                        wrapper.getOrDefault("items", wrapper.getOrDefault("data", Collections.emptyList())));
                if (items != null && !items.isEmpty()) return items;
                // 单个题目
                return Collections.singletonList(wrapper);
            }
        } catch (Exception e) {
            log.debug("题目JSON解析失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    // ==================== 降级题目 ====================

    private Map<String, Object> fallbackQuestion(String subject, String knowledgePoint, String difficulty, int index) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("subject", subject);
        question.put("knowledgePoint", knowledgePoint);
        question.put("difficulty", difficulty);
        question.put("scoreRangeTag", scoreRangeTag(difficulty));

        if (isChoiceSubject(subject)) {
            question.put("questionContent", buildChoiceQuestion(subject, knowledgePoint, difficulty, index));
            question.put("options", generateOptions(subject, knowledgePoint, index));
            question.put("answer", "A");
            question.put("analysis", buildAnalysis(subject, knowledgePoint));
        } else {
            question.put("questionContent", buildOpenQuestion(subject, knowledgePoint, difficulty, index));
            question.put("options", Collections.emptyList());
            question.put("answer", "参考答案：本题围绕" + knowledgePoint + "的核心概念，需要结合教材定义和实例进行分析。");
            question.put("analysis", buildAnalysis(subject, knowledgePoint));
        }
        return question;
    }

    private String buildChoiceQuestion(String subject, String knowledgePoint, String difficulty, int index) {
        return String.format("【%s·%s·%s难度】第%d题：下列关于%s的说法，正确的是（ ）。",
                subject, knowledgePoint, difficultyLabel(difficulty), index, knowledgePoint);
    }

    private String buildOpenQuestion(String subject, String knowledgePoint, String difficulty, int index) {
        return String.format("【%s·%s·%s难度】第%d题：请结合所学知识，分析%s在高考中的考查要点，并举例说明。",
                subject, knowledgePoint, difficultyLabel(difficulty), index, knowledgePoint);
    }

    private List<String> generateOptions(String subject, String knowledgePoint, int index) {
        int seed = Math.abs(knowledgePoint.hashCode() + index);
        String[] prefixes = {"A", "B", "C", "D"};
        String[] contents = {
                String.format("正确表述：%s的核心定义", knowledgePoint),
                String.format("常见误区：混淆%s的相关概念", knowledgePoint),
                String.format("干扰项：%s的逆向表述", knowledgePoint),
                "与本题无关的结论"
        };
        List<String> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            options.add(prefixes[i] + ". " + contents[(seed + i) % 4]);
        }
        return options;
    }

    private String buildAnalysis(String subject, String knowledgePoint) {
        return String.format("本题考查%s中的%s。解题关键在于准确理解该知识点的核心概念，"
                + "区分易混淆的相关内容，并结合教材定义进行判断。", subject, knowledgePoint);
    }

    // ==================== 工具方法 ====================

    private boolean isChoiceSubject(String subject) {
        return Arrays.asList("数学", "英语", "物理", "化学", "生物", "历史", "政治", "地理").contains(subject);
    }

    private int difficultyLevel(String difficulty) {
        return switch (difficulty) {
            case "basic" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> 2;
        };
    }

    private String difficultyLabel(String difficulty) {
        return switch (difficulty) {
            case "basic" -> "基础";
            case "medium" -> "中等";
            case "hard" -> "拔高";
            default -> difficulty;
        };
    }

    private String scoreRangeTag(String difficulty) {
        return switch (difficulty) {
            case "basic" -> "低分段";
            case "medium" -> "中分段";
            case "hard" -> "高分段";
            default -> "通用";
        };
    }

    private boolean notBlank(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty();
    }
}
