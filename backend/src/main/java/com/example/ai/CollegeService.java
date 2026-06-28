package com.example.ai;

import com.example.entity.CollegeBasic;

import java.util.List;
import java.util.Map;

/**
 * 院校素材聚合接口
 * 按需/定时抓取院校官方LOGO、名称、批次
 */
public interface CollegeService {

    /**
     * 刷新院校素材
     */
    boolean refreshColleges();

    /**
     * 根据批次随机获取院校
     */
    List<CollegeBasic> getRandomCollegesByBatch(String admissionBatch, String subjectType, int limit);

    /**
     * 获取所有可用批次列表
     */
    List<String> getBatchList();

    /**
     * 根据院校名称搜索
     */
    List<CollegeBasic> searchCollege(String keyword);

    /**
     * 获取心仪院校信息（含LOGO路径）
     */
    CollegeBasic getDreamCollege(String collegeName);
}
