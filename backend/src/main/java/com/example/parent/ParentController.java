package com.example.parent;

import com.example.dto.response.GrowthDataResponse;
import com.example.exam.ExamService;
import com.example.exam.entity.ExamRecord;
import com.example.growth.GrowthService;
import com.example.learning.LearningService;
import com.example.security.StudentAccessService;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/parent")
public class ParentController {

    @Autowired
    private StudentAccessService studentAccessService;

    @Autowired
    private GrowthService growthService;

    @Autowired
    private LearningService learningService;

    @Autowired
    private ExamService examService;

    /**
     * 获取孩子学习概况
     */
    @GetMapping("/overview/{studentId}")
    public ResponseResult<Map<String, Object>> getChildOverview(@PathVariable Long studentId) {
        studentAccessService.assertReadable(studentId);

        Map<String, Object> overview = new LinkedHashMap<>();
        var cards = growthService.getCollegeCards(studentId);
        var tasks = learningService.getTodayTasks(studentId);
        GrowthDataResponse growthData = growthService.getGrowthData(studentId);
        List<?> examRecords = examService.getExamRecords(studentId);

        overview.put("currentBatch", cards.getCurrentBatch());
        overview.put("dreamCollege", cards.getDreamCollege() == null ? null : cards.getDreamCollege().getName());
        overview.put("scoreGap", cards.getDreamCollege() == null ? null : cards.getDreamCollege().getScoreGap());
        overview.put("weeklyAiComment", tasks.getAiComment());
        overview.put("weeklyCompletionRate", tasks.getCompletionRate());

        // 合并 scoreTrend 和 rankTrend 为 recentExamTrend（含 date, score, rank）
        List<Map<String, Object>> recentExamTrend = mergeTrends(
                growthData.getScoreTrend(),
                growthData.getRankTrend()
        );
        overview.put("recentExamTrend", recentExamTrend);

        return ResponseResult.success(overview);
    }

    /**
     * 合并分数趋势和位次趋势为统一格式 [{date, score, rank}]
     */
    private List<Map<String, Object>> mergeTrends(
            List<GrowthDataResponse.ScoreTrend> scoreTrends,
            List<GrowthDataResponse.RankTrend> rankTrends) {
        List<Map<String, Object>> merged = new ArrayList<>();
        Map<String, Integer> rankByDate = new LinkedHashMap<>();
        if (rankTrends != null) {
            for (GrowthDataResponse.RankTrend rt : rankTrends) {
                rankByDate.put(rt.getDate(), rt.getRank());
            }
        }
        if (scoreTrends != null) {
            for (GrowthDataResponse.ScoreTrend st : scoreTrends) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", st.getDate());
                item.put("score", st.getScore());
                item.put("rank", rankByDate.getOrDefault(st.getDate(), 0));
                merged.add(item);
            }
        }
        return merged;
    }
}
