package com.example.question;

import com.example.entity.CollegeBasic;
import com.example.util.ResponseResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/question")
public class QuestionController {

    /**
     * 获取专项训练题目（AI生成占位）
     */
    @GetMapping("/training/{studentId}")
    public ResponseResult<List<Map<String, Object>>> getTrainingQuestions(@PathVariable Long studentId) {
        List<Map<String, Object>> questions = generateSampleQuestions("training");
        return ResponseResult.success(questions);
    }

    /**
     * 获取补强训练题目（AI生成占位）
     */
    @GetMapping("/reinforcement/{studentId}")
    public ResponseResult<List<Map<String, Object>>> getReinforcementQuestions(@PathVariable Long studentId) {
        List<Map<String, Object>> questions = generateSampleQuestions("reinforcement");
        return ResponseResult.success(questions);
    }

    private List<Map<String, Object>> generateSampleQuestions(String type) {
        List<Map<String, Object>> questions = new ArrayList<>();
        Random random = new Random();

        String[][] templates = {
                {"数学", "函数与导数", "basic", "已知函数 f(x)=x²+2x-3，求 f(2) 的值。", "5"},
                {"数学", "三角函数", "medium", "已知 sin α = 3/5，且 α 为锐角，求 cos 2α 的值。", "-7/25"},
                {"英语", "阅读理解", "basic", "What is the main idea of the passage? (Sample passage: AI in Education)", "C"},
                {"英语", "语法", "medium", "Choose the correct form: She ___ (have/has/had) been studying English for 5 years.", "has"},
                {"语文", "文言文阅读", "basic", "下列句中"之"字用法与其他三项不同的是：", "B"},
                {"历史", "时间轴", "medium", "辛亥革命爆发于哪一年？", "1911"},
                {"地理", "自然地理", "basic", "地球自转一周的时间约为：", "24小时"},
                {"政治", "时事", "medium", "我国的基本经济制度是：", "公有制为主体、多种所有制经济共同发展"},
        };

        int count = 3 + random.nextInt(3);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < templates.length; i++) indices.add(i);
        Collections.shuffle(indices);

        for (int i = 0; i < count && i < indices.size(); i++) {
            int idx = indices.get(i);
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("id", (long) (i + 1));
            question.put("subject", templates[idx][0]);
            question.put("knowledgePoint", templates[idx][1]);
            question.put("difficulty", templates[idx][2]);
            question.put("questionContent", templates[idx][3]);
            question.put("options", Arrays.asList("A. 选项一", "B. 选项二", "C. 选项三", "D. 选项四"));
            question.put("answer", templates[idx][4]);

            if ("reinforcement".equals(type)) {
                question.put("relatedErrorId", (long) (random.nextInt(10) + 1));
            }

            questions.add(question);
        }

        return questions;
    }
}
