package com.example.exam;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.request.ExamSubmitRequest;
import com.example.dto.response.CollegeCardResponse;
import com.example.dto.response.ExamRecordResponse;
import com.example.dto.response.GrowthDataResponse;
import com.example.dto.response.GrowthProgressResponse;
import com.example.exam.entity.ExamRecord;

import java.util.List;

public interface ExamService extends IService<ExamRecord> {

    /**
     * 提交考试成绩，自动换算等效位次
     */
    ExamRecord submitExam(Long studentId, ExamSubmitRequest request);

    /**
     * 获取考试记录列表
     */
    List<ExamRecordResponse> getExamRecords(Long studentId);

    /**
     * 获取考试详情
     */
    ExamRecordResponse getExamDetail(Long examId);

    /**
     * 根据总分计算批次
     */
    String calculateBatch(Integer score);

    /**
     * 获取三段式院校卡片数据
     */
    CollegeCardResponse getCollegeCards(Long studentId);

    /**
     * 获取段位进度条数据
     */
    GrowthProgressResponse getGrowthProgress(Long studentId);

    /**
     * 获取成长数据（总分趋势、位次曲线）
     */
    GrowthDataResponse getGrowthData(Long studentId);
}
