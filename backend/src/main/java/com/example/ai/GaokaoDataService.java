package com.example.ai;

import com.example.entity.ScoreRank;

import java.util.List;

/**
 * 高考数据智能抓取服务
 * 定时任务触发LLM，抓取各省教育考试院一分一段数据
 */
public interface GaokaoDataService {

    /**
     * 同步最新一分一段数据
     */
    boolean syncScoreRankData();

    /**
     * 根据分数查询位次
     */
    Integer findRankByScore(Integer score, String subjectType, Integer year, String province);

    /**
     * 根据位次查询分数
     */
    Integer findScoreByRank(Integer rank, String subjectType, Integer year, String province);

    /**
     * 获取近三年位次平均值
     */
    Integer getAverageRank(List<Integer> scores, String subjectType, String province);

    /**
     * 获取支持的分省列表
     */
    List<String> getSupportedProvinces();
}
