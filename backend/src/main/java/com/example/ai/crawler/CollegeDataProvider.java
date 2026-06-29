package com.example.ai.crawler;

import com.example.entity.CollegeBasic;

import java.util.List;

/**
 * 院校数据提供者接口
 * 通过 LLM 智能获取中国高校录取信息，结合种子数据作为降级方案。
 */
public interface CollegeDataProvider {

    /**
     * 获取指定批次的院校列表
     * @param batch 批次标识：985 / 211 / first_class / second_class
     */
    List<CollegeBasic> getCollegesByBatch(String batch);

    /**
     * 获取院校 LOGO 路径
     * @param collegeName 院校名称
     */
    String getCollegeLogo(String collegeName);
}
