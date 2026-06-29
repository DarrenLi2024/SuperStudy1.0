package com.example.learning;

import com.example.dto.response.KnowledgePointDto;
import com.example.dto.response.TodayTaskResponse;
import com.example.learning.entity.ErrorQuestion;
import com.example.security.StudentAccessService;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    @Autowired
    private LearningService learningService;

    @Autowired
    private StudentAccessService studentAccessService;

    /**
     * 获取今日任务
     */
    @GetMapping("/today/{studentId}")
    public ResponseResult<TodayTaskResponse> getTodayTasks(@PathVariable Long studentId) {
        studentAccessService.assertReadable(studentId);
        TodayTaskResponse response = learningService.getTodayTasks(studentId);
        return ResponseResult.success(response);
    }

    /**
     * 完成任务
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseResult<Void> completeTask(@RequestParam Long taskId,
                                             @RequestParam BigDecimal completionRate) {
        learningService.completeTask(taskId, completionRate);
        return ResponseResult.success();
    }

    /**
     * 获取错题列表
     */
    @GetMapping("/errors/{studentId}")
    public ResponseResult<List<ErrorQuestion>> getErrorQuestions(@PathVariable Long studentId) {
        studentAccessService.assertReadable(studentId);
        List<ErrorQuestion> errors = learningService.getErrorQuestions(studentId);
        return ResponseResult.success(errors);
    }

    /**
     * 记录错题
     */
    @PostMapping("/errors")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseResult<Void> recordError(@RequestBody ErrorQuestion question) {
        Long studentId = studentAccessService.requireCurrentStudentId();
        question.setStudentId(studentId);
        learningService.recordErrorQuestion(question);
        return ResponseResult.success();
    }

    /**
     * 获取知识点掌握情况
     */
    @GetMapping("/knowledge/{studentId}")
    public ResponseResult<Map<String, List<KnowledgePointDto>>> getKnowledgeStatus(@PathVariable Long studentId) {
        studentAccessService.assertReadable(studentId);
        Map<String, List<KnowledgePointDto>> result = learningService.getKnowledgeStatus(studentId);
        return ResponseResult.success(result);
    }
}
