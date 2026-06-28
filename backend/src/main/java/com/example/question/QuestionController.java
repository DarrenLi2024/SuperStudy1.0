package com.example.question;

import com.example.ai.QuestionBankService;
import com.example.security.StudentAccessService;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/question")
public class QuestionController {

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private StudentAccessService studentAccessService;

    /**
     * 获取专项训练题目
     */
    @GetMapping("/training/{studentId}")
    public ResponseResult<List<Map<String, Object>>> getTrainingQuestions(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "数学") String subject,
            @RequestParam(defaultValue = "5") Integer count) {
        studentAccessService.assertReadable(studentId);
        List<Map<String, Object>> questions = questionBankService.generateTrainingQuestions(studentId, subject, count);
        return ResponseResult.success(questions);
    }

    /**
     * 获取补强训练题目
     */
    @GetMapping("/reinforcement/{studentId}")
    public ResponseResult<List<Map<String, Object>>> getReinforcementQuestions(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "函数与导数") String knowledgePoint,
            @RequestParam(defaultValue = "5") Integer count) {
        studentAccessService.assertReadable(studentId);
        List<Map<String, Object>> questions = questionBankService.generateReinforcementQuestions(studentId, knowledgePoint, count);
        return ResponseResult.success(questions);
    }
}
