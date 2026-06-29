package com.example.ai;

import com.example.ai.client.LlmClient;
import com.example.exam.mapper.ExamRecordMapper;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.student.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LearningAnalysisServiceTest {

    @Test
    void calculateSubjectWeights_extremeMathWeakness_allocatesFortyPercent() {
        ExamRecordMapper examRecordMapper = mock(ExamRecordMapper.class);
        ErrorQuestionMapper errorQuestionMapper = mock(ErrorQuestionMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        LlmClient llmClient = mock(LlmClient.class);

        ErrorQuestion error1 = new ErrorQuestion();
        error1.setSubject("数学");
        error1.setKnowledgePoint("函数与导数");
        ErrorQuestion error2 = new ErrorQuestion();
        error2.setSubject("数学");
        error2.setKnowledgePoint("函数与导数");

        when(examRecordMapper.selectOne(any())).thenReturn(null);
        when(errorQuestionMapper.selectList(any())).thenReturn(Arrays.asList(error1, error2));

        LearningAnalysisServiceImpl service = new LearningAnalysisServiceImpl(
                examRecordMapper, errorQuestionMapper, studentProfileMapper, llmClient, new ObjectMapper());

        var weights = service.calculateSubjectWeights(1L);

        assertTrue(weights.get("数学") >= 0.4);
    }
}
