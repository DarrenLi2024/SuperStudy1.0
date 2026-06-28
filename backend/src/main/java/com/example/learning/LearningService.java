package com.example.learning;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.response.TodayTaskResponse;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.entity.TaskRecord;

import java.util.List;
import java.util.Map;

public interface LearningService extends IService<TaskRecord> {

    /**
     * 获取今日学习任务
     */
    TodayTaskResponse getTodayTasks(Long studentId);

    /**
     * 完成任务
     */
    void completeTask(Long taskId, java.math.BigDecimal completionRate);

    /**
     * 获取错题列表
     */
    List<ErrorQuestion> getErrorQuestions(Long studentId);

    /**
     * 记录错题
     */
    void recordErrorQuestion(ErrorQuestion question);

    /**
     * 获取知识点掌握情况
     */
    Map<String, List<KnowledgePointDto>> getKnowledgeStatus(Long studentId);
}
