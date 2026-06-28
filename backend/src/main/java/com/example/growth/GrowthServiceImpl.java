package com.example.growth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.response.CollegeCardResponse;
import com.example.dto.response.GrowthDataResponse;
import com.example.dto.response.GrowthProgressResponse;
import com.example.exam.ExamService;
import com.example.growth.entity.GrowthRecord;
import com.example.growth.mapper.GrowthRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrowthServiceImpl extends ServiceImpl<GrowthRecordMapper, GrowthRecord> implements GrowthService {

    @Autowired
    private ExamService examService;

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
        return examService.getGrowthData(studentId);
    }

    @Override
    public List<GrowthRecord> getGrowthHistory(Long studentId) {
        LambdaQueryWrapper<GrowthRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GrowthRecord::getStudentId, studentId)
                .orderByDesc(GrowthRecord::getUpgradeTime);
        return this.list(queryWrapper);
    }
}
