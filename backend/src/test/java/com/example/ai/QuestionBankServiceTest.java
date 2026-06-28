package com.example.ai;

import com.example.ai.generator.QuestionGenerator;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.mapper.AiQuestionBankMapper;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionBankServiceTest {

    @Test
    void generateTrainingQuestions_lowScoreStudent_prefersBasicQuestions() {
        AiQuestionBankMapper questionBankMapper = mock(AiQuestionBankMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        ErrorQuestionMapper errorQuestionMapper = mock(ErrorQuestionMapper.class);
        QuestionGenerator questionGenerator = mock(QuestionGenerator.class);

        StudentProfile profile = new StudentProfile();
        profile.setId(1L);
        profile.setBaselineScore(450);
        when(studentProfileMapper.selectById(1L)).thenReturn(profile);
        when(errorQuestionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(questionBankMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(questionBankMapper.insert(any())).thenAnswer(invocation -> 1);
        when(questionGenerator.generateBatch(anyString(), anyString(), anyInt())).thenAnswer(invocation -> {
            String subject = invocation.getArgument(0);
            String difficulty = invocation.getArgument(1);
            int count = invocation.getArgument(2);
            List<Map<String, Object>> questions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Object> question = new LinkedHashMap<>();
                question.put("subject", subject);
                question.put("knowledgePoint", "函数与导数");
                question.put("difficulty", difficulty);
                question.put("questionContent", "测试题");
                question.put("options", Arrays.asList("A", "B"));
                question.put("answer", "A");
                question.put("analysis", "解析");
                questions.add(question);
            }
            return questions;
        });
        when(questionGenerator.validateQuestion(any())).thenReturn(true);

        QuestionBankServiceImpl service = new QuestionBankServiceImpl(
                questionBankMapper, studentProfileMapper, errorQuestionMapper, questionGenerator, new ObjectMapper());

        List<Map<String, Object>> questions = service.generateTrainingQuestions(1L, "数学", 5);

        long basicCount = questions.stream().filter(q -> "basic".equals(q.get("difficulty"))).count();
        assertEquals(5, questions.size());
        assertTrue(basicCount >= 3);
    }
}
