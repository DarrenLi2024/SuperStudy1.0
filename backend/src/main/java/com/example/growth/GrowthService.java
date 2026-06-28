package com.example.growth;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.response.CollegeCardResponse;
import com.example.dto.response.GrowthDataResponse;
import com.example.dto.response.GrowthProgressResponse;
import com.example.growth.entity.GrowthRecord;

import java.util.List;

public interface GrowthService extends IService<GrowthRecord> {

    /**
     * 获取三段式院校卡片数据
     */
    CollegeCardResponse getCollegeCards(Long studentId);

    /**
     * 获取段位进度条数据
     */
    GrowthProgressResponse getGrowthProgress(Long studentId);

    /**
     * 获取成长数据（总分趋势、位次曲线、单科曲线）
     */
    GrowthDataResponse getGrowthData(Long studentId);

    /**
     * 获取段位升级历史记录
     */
    List<GrowthRecord> getGrowthHistory(Long studentId);
}
