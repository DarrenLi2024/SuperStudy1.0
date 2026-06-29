package com.example.ai.crawler;

import com.example.entity.ScoreRank;

import java.util.List;

/**
 * 高考一分一段数据提供者接口
 * 通过 LLM 根据训练数据生成分数-位次对应关系，结合线性估算作为降级方案。
 * 
 * 注意：这不是网页爬虫，而是基于 LLM 知识库的数据生成。
 * 真实一分一段数据应从各省教育考试院官网获取后人工导入。
 */
public interface GaokaoDataProvider {

    /**
     * 获取指定省份、年份、科类的一分一段数据
     * @param province 省份名称
     * @param year 年份
     * @param subjectType 科类：physics(物理类) / history(历史类)
     */
    List<ScoreRank> getScoreRank(String province, int year, String subjectType);

    /**
     * 获取支持的所有省份列表
     */
    List<String> getSupportedProvinces();
}
