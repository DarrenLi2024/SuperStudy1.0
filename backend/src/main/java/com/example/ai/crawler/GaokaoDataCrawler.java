package com.example.ai.crawler;

import com.example.entity.ScoreRank;

import java.util.List;

/**
 * 高考数据爬虫
 * 对接LLM API进行数据抓取和结构化
 * 当前为占位实现
 */
public interface GaokaoDataCrawler {

    /**
     * 抓取指定省份和年份的一分一段数据
     */
    List<ScoreRank> crawlScoreRank(String province, int year, String subjectType);

    /**
     * 获取支持的省份列表
     */
    List<String> getSupportedProvinces();
}
