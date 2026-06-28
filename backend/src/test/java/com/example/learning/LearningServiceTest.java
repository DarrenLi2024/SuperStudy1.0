package com.example.learning;

import com.example.learning.entity.ErrorQuestion;
import com.example.learning.entity.TaskRecord;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.learning.mapper.TaskRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private TaskRecordMapper taskRecordMapper;

    @Mock
    private ErrorQuestionMapper errorQuestionMapper;

    @InjectMocks
    private LearningServiceImpl learningService;

    @BeforeEach
    void setUp() {
        injectField(learningService, "baseMapper", taskRecordMapper);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Nested
    @DisplayName("今日任务测试")
    class TodayTaskTests {

        @Test
        @DisplayName("无今日任务时生成默认任务")
        void getTodayTasks_NoExisting_GeneratesDefault() {
            when(taskRecordMapper.selectOne(any())).thenReturn(null);

            var result = learningService.getTodayTasks(1L);

            assertNotNull(result);
            assertNotNull(result.getTasks());
            assertTrue(result.getTasks().size() >= 3);
            assertNotNull(result.getAiComment());
            assertEquals(0.0, result.getCompletionRate());
        }
    }

    @Nested
    @DisplayName("错题管理测试")
    class ErrorQuestionTests {

        @Test
        @DisplayName("记录错题成功")
        void recordErrorQuestion_Success() {
            ErrorQuestion question = new ErrorQuestion();
            question.setStudentId(1L);
            question.setSubject("数学");
            question.setKnowledgePoint("函数与导数");
            question.setQuestionContent("测试题目内容");
            question.setWrongAnswer("错误答案");
            question.setAiAnalysis("测试分析");

            learningService.recordErrorQuestion(question);

            verify(errorQuestionMapper).insert(question);
            assertEquals(0, question.getReinforcementFlag());
        }

        @Test
        @DisplayName("获取错题列表")
        void getErrorQuestions_Success() {
            ErrorQuestion q1 = new ErrorQuestion();
            q1.setId(1L);
            q1.setSubject("数学");
            when(errorQuestionMapper.selectList(any())).thenReturn(Collections.singletonList(q1));

            var errors = learningService.getErrorQuestions(1L);

            assertNotNull(errors);
            assertEquals(1, errors.size());
            assertEquals("数学", errors.get(0).getSubject());
        }
    }

    @Nested
    @DisplayName("知识点掌握度测试")
    class KnowledgeStatusTests {

        @Test
        @DisplayName("无错题时返回默认掌握度")
        void getKnowledgeStatus_NoErrors_ReturnsDefault() {
            when(errorQuestionMapper.selectList(any())).thenReturn(Collections.emptyList());

            var result = learningService.getKnowledgeStatus(1L);

            assertNotNull(result);
            assertTrue(result.containsKey("语文"));
            assertTrue(result.containsKey("数学"));
            assertTrue(result.containsKey("英语"));
        }

        @Test
        @DisplayName("有错题时按错题分析掌握度")
        void getKnowledgeStatus_WithErrors_Analyzes() {
            ErrorQuestion mathError = new ErrorQuestion();
            mathError.setSubject("数学");
            mathError.setKnowledgePoint("函数与导数");
            mathError.setQuestionContent("测试题");
            mathError.setAiAnalysis("错因分析");

            when(errorQuestionMapper.selectList(any())).thenReturn(Collections.singletonList(mathError));

            var result = learningService.getKnowledgeStatus(1L);

            assertNotNull(result);
            assertTrue(result.containsKey("数学"));
        }
    }

    @Nested
    @DisplayName("完成任务测试")
    class CompleteTaskTests {

        @Test
        @DisplayName("完成任务（完成率>80%）")
        void completeTask_Completed() {
            TaskRecord task = new TaskRecord();
            task.setId(1L);
            task.setStatus("pending");
            when(taskRecordMapper.selectById(1L)).thenReturn(task);

            learningService.completeTask(1L, BigDecimal.valueOf(85));

            assertEquals("completed", task.getStatus());
            verify(taskRecordMapper).updateById(task);
        }

        @Test
        @DisplayName("部分完成任务")
        void completeTask_Partial() {
            TaskRecord task = new TaskRecord();
            task.setId(1L);
            task.setStatus("pending");
            when(taskRecordMapper.selectById(1L)).thenReturn(task);

            learningService.completeTask(1L, BigDecimal.valueOf(50));

            assertEquals("partial", task.getStatus());
            verify(taskRecordMapper).updateById(task);
        }
    }
}
