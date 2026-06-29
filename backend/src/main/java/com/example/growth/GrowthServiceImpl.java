package com.example.growth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.IncentiveService;
import com.example.ai.LearningAnalysisService;
import com.example.dto.response.CollegeCardResponse;
import com.example.dto.response.GrowthDataResponse;
import com.example.dto.response.GrowthProgressResponse;
import com.example.exam.ExamService;
import com.example.growth.entity.GrowthRecord;
import com.example.growth.mapper.GrowthRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class GrowthServiceImpl extends ServiceImpl<GrowthRecordMapper, GrowthRecord> implements GrowthService {

    @Autowired
    private ExamService examService;

    @Autowired(required = false)
    private LearningAnalysisService learningAnalysisService;

    @Autowired(required = false)
    private IncentiveService incentiveService;

    @Override
    public CollegeCardResponse getCollegeCards(Long studentId) {
        return examService.getCollegeCards(studentId);
    }

    @Override
    public GrowthProgressResponse getGrowthProgress(Long studentId) {
        return examService.getGrowthProgress(studentId);
    }

    @Override
    public GrowthDataResponse getGrowthData(Long studentId) {
        GrowthDataResponse data = examService.getGrowthData(studentId);

        // 尝试通过 LLM 生成月度报告覆盖默认值
        if (learningAnalysisService != null && data != null) {
            try {
                String aiReport = learningAnalysisService.generateMonthlyReport(studentId);
                if (aiReport != null && aiReport.length() > 10 && !aiReport.contains("本周期主要短板为")) {
                    data.setMonthlyReport(aiReport);
                }
            } catch (Exception e) {
                log.debug("LLM月度报告生成失败: {}", e.getMessage());
            }
        }

        return data;
    }

    @Override
    public List<GrowthRecord> getGrowthHistory(Long studentId) {
        LambdaQueryWrapper<GrowthRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GrowthRecord::getStudentId, studentId)
                .orderByDesc(GrowthRecord::getUpgradeTime);
        return this.list(queryWrapper);
    }
}
