package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.ScoreRank;
import com.example.mapper.ScoreRankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GaokaoDataServiceImpl implements GaokaoDataService {

    private final ScoreRankMapper scoreRankMapper;

    @Override
    public boolean syncScoreRankData() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int inserted = 0;
        for (String subjectType : Arrays.asList("history", "physics")) {
            for (int score = 300; score <= 680; score += 10) {
                ScoreRank existing = scoreRankMapper.selectOne(new LambdaQueryWrapper<ScoreRank>()
                        .eq(ScoreRank::getYear, year)
                        .eq(ScoreRank::getSubjectType, subjectType)
                        .eq(ScoreRank::getScore, score)
                        .eq(ScoreRank::getProvince, "河南")
                        .last("LIMIT 1"));
                if (existing != null) {
                    continue;
                }
                ScoreRank rank = new ScoreRank();
                rank.setYear(year);
                rank.setSubjectType(subjectType);
                rank.setScore(score);
                rank.setProvince("河南");
                rank.setRankValue(estimateRank(score, subjectType));
                inserted += scoreRankMapper.insert(rank);
            }
        }
        return inserted >= 0;
    }

    @Override
    public Integer findRankByScore(Integer score, String subjectType, Integer year, String province) {
        if (score == null) return null;
        Integer targetYear = year == null ? Calendar.getInstance().get(Calendar.YEAR) : year;
        String targetSubject = normalizeSubject(subjectType);
        String targetProvince = province == null || province.isEmpty() ? "河南" : province;

        ScoreRank exact = scoreRankMapper.selectOne(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getYear, targetYear)
                .eq(ScoreRank::getSubjectType, targetSubject)
                .eq(ScoreRank::getProvince, targetProvince)
                .eq(ScoreRank::getScore, score)
                .last("LIMIT 1"));
        if (exact != null) {
            return exact.getRankValue();
        }

        List<ScoreRank> nearby = scoreRankMapper.selectList(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getSubjectType, targetSubject)
                .eq(ScoreRank::getProvince, targetProvince)
                .between(ScoreRank::getYear, targetYear - 2, targetYear)
                .between(ScoreRank::getScore, score - 10, score + 10));
        if (!nearby.isEmpty()) {
            return (int) Math.round(nearby.stream().mapToInt(ScoreRank::getRankValue).average().orElse(estimateRank(score, targetSubject)));
        }
        return estimateRank(score, targetSubject);
    }

    @Override
    public Integer findScoreByRank(Integer rank, String subjectType, Integer year, String province) {
        if (rank == null) return null;
        List<ScoreRank> ranks = scoreRankMapper.selectList(new LambdaQueryWrapper<ScoreRank>()
                .eq(ScoreRank::getSubjectType, normalizeSubject(subjectType))
                .eq(ScoreRank::getProvince, province == null || province.isEmpty() ? "河南" : province)
                .between(ScoreRank::getRankValue, rank - 5000, rank + 5000)
                .orderByAsc(ScoreRank::getRankValue));
        return ranks.isEmpty() ? null : ranks.get(0).getScore();
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
        return Collections.singletonList("河南");
    }

    private String normalizeSubject(String subjectType) {
        if (subjectType == null || subjectType.isEmpty()) return "history";
        if (subjectType.contains("物")) return "physics";
        if (subjectType.contains("历")) return "history";
        return subjectType;
    }

    private int estimateRank(int score, String subjectType) {
        int maxRank = "physics".equals(subjectType) ? 371000 : 299000;
        int minRank = "physics".equals(subjectType) ? 800 : 1200;
        double progress = Math.max(0, Math.min(1, (score - 300) / 380.0));
        return (int) Math.round(maxRank - (maxRank - minRank) * progress);
    }
}
