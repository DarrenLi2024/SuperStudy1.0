package com.example.growth;

import com.example.dto.response.*;
import com.example.exam.ExamService;
import com.example.exam.entity.ExamRecord;
import com.example.growth.entity.GrowthRecord;
import com.example.growth.mapper.GrowthRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthServiceTest {

    @Mock
    private GrowthRecordMapper growthRecordMapper;

    @Mock
    private ExamService examService;

    @InjectMocks
    private GrowthServiceImpl growthService;

    @BeforeEach
    void setUp() {
        injectField(growthService, "baseMapper", growthRecordMapper);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("获取院校卡片 - 委托给ExamService")
    void getCollegeCards_DelegatesToExamService() {
        CollegeCardResponse mockResponse = CollegeCardResponse.builder()
                .currentBatch("公办二本")
                .currentScore(450)
                .build();
        when(examService.getCollegeCards(1L)).thenReturn(mockResponse);

        var result = growthService.getCollegeCards(1L);

        assertNotNull(result);
        assertEquals("公办二本", result.getCurrentBatch());
        assertEquals(450, result.getCurrentScore());
        verify(examService).getCollegeCards(1L);
    }

    @Test
    @DisplayName("获取段位进度 - 委托给ExamService")
    void getGrowthProgress_DelegatesToExamService() {
        GrowthProgressResponse mockResponse = GrowthProgressResponse.builder()
                .targetScore(600)
                .totalProgress(20.0)
                .build();
        when(examService.getGrowthProgress(1L)).thenReturn(mockResponse);

        var result = growthService.getGrowthProgress(1L);

        assertNotNull(result);
        assertEquals(600, result.getTargetScore());
        assertEquals(20.0, result.getTotalProgress());
    }

    @Test
    @DisplayName("获取成长数据 - 委托给ExamService")
    void getGrowthData_DelegatesToExamService() {
        GrowthDataResponse mockResponse = GrowthDataResponse.builder()
                .scoreTrend(Collections.emptyList())
                .monthlyReport("测试报告")
                .build();
        when(examService.getGrowthData(1L)).thenReturn(mockResponse);

        var result = growthService.getGrowthData(1L);

        assertNotNull(result);
        assertEquals("测试报告", result.getMonthlyReport());
    }

    @Test
    @DisplayName("获取升级历史 - 调用Mapper")
    void getGrowthHistory_Success() {
        GrowthRecord record = new GrowthRecord();
        record.setId(1L);
        record.setStudentId(1L);
        record.setPreviousBatch("本科以下");
        record.setCurrentBatch("公办二本");
        record.setScoreAtUpgrade(400);
        when(growthRecordMapper.selectList(any())).thenReturn(Collections.singletonList(record));

        var history = growthService.getGrowthHistory(1L);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("本科以下", history.get(0).getPreviousBatch());
        assertEquals("公办二本", history.get(0).getCurrentBatch());
    }
}
