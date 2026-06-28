package com.example.ai.crawler;

import com.example.entity.CollegeBasic;

import java.util.List;

/**
 * 院校素材爬虫
 * 抓取院校官方LOGO、名称、批次
 * 当前为占位实现
 */
public interface CollegeCrawler {

    /**
     * 抓取指定批次的院校列表
     */
    List<CollegeBasic> crawlCollegesByBatch(String batch);

    /**
     * 抓取院校LOGO
     */
    String crawlCollegeLogo(String collegeName);
}
