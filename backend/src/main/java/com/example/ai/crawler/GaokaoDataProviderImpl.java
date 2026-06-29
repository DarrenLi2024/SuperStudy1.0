package com.example.ai.crawler;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.example.entity.ScoreRank;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高考一分一段数据提供者实现
 * 通过 LLM 根据训练知识生成分数-位次对应数据，未配置远程 AI 时使用线性估算。
 * 
 * 重要声明：这不是网页爬虫。真实一分一段数据应从各省教育考试院官网获取。
 * 当前 LLM 生成的数据仅供参考，用于功能演示和降级场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GaokaoDataProviderImpl implements GaokaoDataProvider {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /** 支持的所有省份 */
    private static final List<String> SUPPORTED_PROVINCES = Arrays.asList(
            "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
            "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东",
            "河南", "湖北", "湖南", "广东", "广西", "海南",
            "重庆", "四川", "贵州", "云南", "西藏",
            "陕西", "甘肃", "青海", "宁夏", "新疆"
    );

    /** 一分一段关键分数点 */
    private static final int[] KEY_SCORES = {
            750, 740, 730, 720, 710, 700, 690, 680, 670, 660, 650, 640, 630, 620, 610, 600,
            590, 580, 570, 560, 550, 540, 530, 520, 510, 500,
            490, 480, 470, 460, 450, 440, 430, 420, 410, 400,
            390, 380, 370, 360, 350, 340, 330, 320, 310, 300,
            280, 260, 240, 220, 200
    };

    @Override
    public List<ScoreRank> getScoreRank(String province, int year, String subjectType) {
        String subTypeLabel = "physics".equals(subjectType) ? "物理类" : "历史类";
        log.info("获取一分一段数据: {} {}年 {}", province, year, subTypeLabel);

        LlmRequest request = LlmRequest.builder()
                .taskType("score_rank_lookup")
                .systemPrompt(buildSystemPrompt())
                .userPrompt(buildPrompt(province, year, subjectType))
                .responseSchema(buildResponseSchema())
                .temperature(0.1)
                .maxTokens(4096)
                .skipCache(true)
                .build();

        try {
            LlmResponse response = llmClient.generate(request);
            if (response.isFallback() || response.getContent() == null || "{}".equals(response.getContent())) {
                log.info("LLM一分一段数据获取降级，使用线性估算: {} {}", province, year);
                return estimateScoreRank(province, year, subjectType);
            }

            String content = response.getContent().trim();
            List<Map<String, Object>> items;

            if (content.startsWith("[")) {
                items = objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
            } else {
                Map<String, Object> wrapper = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) wrapper.getOrDefault("items",
                        wrapper.getOrDefault("data", Collections.emptyList()));
                items = list;
            }

            if (items == null || items.isEmpty()) {
                log.info("LLM返回空一分一段数据，使用估算: {} {}", province, year);
                return estimateScoreRank(province, year, subjectType);
            }

            List<ScoreRank> ranks = items.stream()
                    .map(item -> toScoreRank(item, province, year, subjectType))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("成功获取一分一段数据: {} {}年 {} 共{}条", province, year, subTypeLabel, ranks.size());
            return ranks;

        } catch (Exception e) {
            log.error("一分一段数据获取异常: {} {} {} - {}", province, year, subjectType, e.getMessage());
            return estimateScoreRank(province, year, subjectType);
        }
    }

    @Override
    public List<String> getSupportedProvinces() {
        return SUPPORTED_PROVINCES;
    }

    // ==================== Prompt构建 ====================

    private String buildSystemPrompt() {
        return """
                你是一位中国高考数据分析专家。你需要根据中国各省教育考试院公布的一分一段表数据模式，
                生成指定省份、年份、科类的高考分数-位次对应数据。

                ## 数据规则
                1. 分数范围：750-200分，重点覆盖高分段（每1分一档）和中低分段（每5-10分一档）
                2. 位次规律：
                   - 理科(物理类)通常比文科(历史类)考生多，同位次分数更高
                   - 人口大省（河南、广东、山东、四川等）同位次竞争更激烈
                   - 985线约前1-2%，211线约前3-8%，一本线约前15-25%
                3. 输出严格JSON数组格式

                ## 重要声明
                - 数据为基于公开统计规律的合理估算，并非官方精确数据
                - 真实一分一段数据请以各省教育考试院官方发布为准

                ## 安全约束
                - 数据应为合理估算，不得伪造官方数据
                - 不得包含任何政治敏感内容
                """;
    }

    private String buildPrompt(String province, int year, String subjectType) {
        String subTypeLabel = "physics".equals(subjectType) ? "物理类" : "历史类";
        return String.format("""
                请生成%s省%d年高考%s一分一段数据（关键分数点）。

                要求覆盖以下分数段的关键位次：
                - 750-600分：每5分一个数据点
                - 600-500分：每10分一个数据点
                - 500-300分：每20分一个数据点
                - 300-200分：每50分一个数据点

                请输出JSON数组，每条包含：score(分数), rankValue(累计位次), province(省份), year(年份), subjectType(科类)
                """, province, year, subTypeLabel);
    }

    private String buildResponseSchema() {
        return """
                [
                  {
                    "score": 680,
                    "rankValue": 1500,
                    "province": "河南",
                    "year": 2025,
                    "subjectType": "physics"
                  }
                ]""";
    }

    // ==================== 数据转换 ====================

    private ScoreRank toScoreRank(Map<String, Object> item, String province, int year, String subjectType) {
        try {
            ScoreRank rank = new ScoreRank();
            rank.setScore(toInt(item.get("score")));
            rank.setRankValue(toInt(item.get("rankValue")));
            rank.setProvince(String.valueOf(item.getOrDefault("province", province)));
            rank.setYear(toInt(item.getOrDefault("year", year)));
            rank.setSubjectType(String.valueOf(item.getOrDefault("subjectType", subjectType)));

            if (rank.getScore() == null || rank.getRankValue() == null) {
                return null;
            }
            return rank;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 估算降级 ====================

    private List<ScoreRank> estimateScoreRank(String province, int year, String subjectType) {
        List<ScoreRank> ranks = new ArrayList<>();
        int maxRank = "physics".equals(subjectType) ? 380000 : 310000;
        int minRank = "physics".equals(subjectType) ? 500 : 800;

        for (int score : KEY_SCORES) {
            double progress = Math.max(0, Math.min(1, (score - 200.0) / 550.0));
            int rank = (int) Math.round(maxRank - (maxRank - minRank) * progress);

            ScoreRank sr = new ScoreRank();
            sr.setScore(score);
            sr.setRankValue(rank);
            sr.setProvince(province);
            sr.setYear(year);
            sr.setSubjectType(subjectType);
            ranks.add(sr);
        }
        return ranks;
    }
}
