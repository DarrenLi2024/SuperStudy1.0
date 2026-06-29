package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.crawler.GaokaoDataCrawler;
import com.example.entity.ScoreRank;
import com.example.mapper.ScoreRankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GaokaoDataServiceImpl implements GaokaoDataService {

    private final ScoreRankMapper scoreRankMapper;
    private final GaokaoDataCrawler crawler;

    @Override
    public boolean syncScoreRankData() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // 优先同步河南省（用户主要省份），再扩展到全国
        List<String> provinces = new ArrayList<>();
        provinces.add("河南");

        // 检查是否已有足够数据，若无则扩展到其他省份
        long existingCount = scoreRankMapper.selectCount(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getYear, currentYear));
        if (existingCount < 100) {
            // 数据不足，扩展到更多省份
            provinces.addAll(Arrays.asList("广东", "山东", "四川", "江苏", "河北", "湖北", "湖南", "浙江"));
        }

        int totalInserted = 0;
        for (String province : provinces) {
            for (String subjectType : Arrays.asList("physics", "history")) {
                try {
                    List<ScoreRank> ranks = crawler.crawlScoreRank(province, currentYear, subjectType);
                    int inserted = saveRanks(ranks);
                    totalInserted += inserted;
                    log.info("一分一段同步完成: {} {}年 {} 插入{}条", province, currentYear,
                            "physics".equals(subjectType) ? "物理类" : "历史类", inserted);
                } catch (Exception e) {
                    log.error("一分一段同步失败: {} {} {} - {}", province, currentYear, subjectType, e.getMessage());
                }
            }
        }
        log.info("一分一段数据同步完成，共插入{}条", totalInserted);
        return true;
    }

    @Override
    public Integer findRankByScore(Integer score, String subjectType, Integer year, String province) {
        if (score == null) return null;
        Integer targetYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        String targetSubject = normalizeSubject(subjectType);
        String targetProvince = province == null || province.isEmpty() ? "河南" : province;

        // 精确匹配
        ScoreRank exact = scoreRankMapper.selectOne(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getYear, targetYear)
                .eq(ScoreRank::getSubjectType, targetSubject)
                .eq(ScoreRank::getProvince, targetProvince)
                .eq(ScoreRank::getScore, score)
                .last("LIMIT 1"));
        if (exact != null) return exact.getRankValue();

        // 邻近插值
        List<ScoreRank> nearby = scoreRankMapper.selectList(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getSubjectType, targetSubject)
                .eq(ScoreRank::getProvince, targetProvince)
                .between(ScoreRank::getYear, targetYear - 2, targetYear)
                .between(ScoreRank::getScore, Math.max(0, score - 20), score + 20));
        if (!nearby.isEmpty()) {
            return (int) Math.round(nearby.stream()
                    .mapToInt(ScoreRank::getRankValue)
                    .average()
                    .orElse(estimateRank(score, targetSubject)));
        }
        return estimateRank(score, targetSubject);
    }

    @Override
    public Integer findScoreByRank(Integer rank, String subjectType, Integer year, String province) {
        if (rank == null) return null;
        Integer targetYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        String targetProvince = province == null || province.isEmpty() ? "河南" : province;

        List<ScoreRank> ranks = scoreRankMapper.selectList(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getSubjectType, normalizeSubject(subjectType))
                .eq(ScoreRank::getProvince, targetProvince)
                .eq(ScoreRank::getYear, targetYear)
                .between(ScoreRank::getRankValue, rank - 5000, rank + 5000)
                .orderByAsc(ScoreRank::getRankValue));

        if (ranks.isEmpty()) {
            // 尝试邻近年份
            ranks = scoreRankMapper.selectList(new LambdaQueryWrapper<ScoreRank>()
                    .eq(ScoreRank::getSubjectType, normalizeSubject(subjectType))
                    .eq(ScoreRank::getProvince, targetProvince)
                    .between(ScoreRank::getYear, targetYear - 2, targetYear)
                    .between(ScoreRank::getRankValue, rank - 5000, rank + 5000)
                    .orderByAsc(ScoreRank::getRankValue));
        }

        if (ranks.isEmpty()) return null;

        // 线性插值找到最接近的分数
        ScoreRank closest = ranks.get(0);
        int minDiff = Math.abs(closest.getRankValue() - rank);
        for (ScoreRank r : ranks) {
            int diff = Math.abs(r.getRankValue() - rank);
            if (diff < minDiff) {
                minDiff = diff;
                closest = r;
            }
        }
        return closest.getScore();
    }

    @Override
    public Integer getAverageRank(List<Integer> scores, String subjectType, String province) {
        if (scores == null || scores.isEmpty()) return null;
        List<Integer> ranks = scores.stream()
                .map(score -> findRankByScore(score, subjectType, null, province))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ranks.isEmpty() ? null : (int) Math.round(ranks.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    @Override
    public List<String> getSupportedProvinces() {
        return crawler.getSupportedProvinces();
    }

    // ==================== 内部方法 ====================

    private int saveRanks(List<ScoreRank> ranks) {
        int inserted = 0;
        for (ScoreRank rank : ranks) {
            // 检查是否已存在
            ScoreRank existing = scoreRankMapper.selectOne(new LambdaQueryWrapper<ScoreRank>()
                    .eq(ScoreRank::getProvince, rank.getProvince())
                    .eq(ScoreRank::getYear, rank.getYear())
                    .eq(ScoreRank::getSubjectType, rank.getSubjectType())
                    .eq(ScoreRank::getScore, rank.getScore())
                    .last("LIMIT 1"));
            if (existing == null && rank.getScore() != null && rank.getRankValue() != null) {
                inserted += scoreRankMapper.insert(rank);
            }
        }
        return inserted;
    }

    private String normalizeSubject(String subjectType) {
        if (subjectType == null || subjectType.isEmpty()) return "history";
        if (subjectType.contains("物") || subjectType.contains("理")) return "physics";
        if (subjectType.contains("历") || subjectType.contains("史")) return "history";
        return subjectType;
    }

    private int estimateRank(int score, String subjectType) {
        int maxRank = "physics".equals(subjectType) ? 371000 : 299000;
        int minRank = "physics".equals(subjectType) ? 800 : 1200;
        double progress = Math.max(0, Math.min(1, (score - 200.0) / 550.0));
        return (int) Math.round(maxRank - (maxRank - minRank) * progress);
    }
}
