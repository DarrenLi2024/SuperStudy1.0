package com.example.parent;

import com.example.util.ResponseResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/parent")
public class ParentController {

    /**
     * 获取孩子学习概况
     */
    @GetMapping("/overview/{studentId}")
    public ResponseResult<Map<String, Object>> getChildOverview(@PathVariable Long studentId) {
        Map<String, Object> overview = new LinkedHashMap<>();

        overview.put("currentBatch", "公办二本");
        overview.put("dreamCollege", "目标院校");
        overview.put("scoreGap", 120);

        overview.put("weeklyAiComment", "本周学习表现良好，数学仍有较大提升空间，建议加强函数与导数模块的专项训练。");
        overview.put("weeklyCompletionRate", 78.5);

        List<Map<String, Object>> trend = new ArrayList<>();
        String[] dates = {"2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22"};
        int[] scores = {420, 435, 450, 445};
        int[] ranks = {85000, 82000, 78000, 79500};
        for (int i = 0; i < dates.length; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dates[i]);
            point.put("score", scores[i]);
            point.put("rank", ranks[i]);
            trend.add(point);
        }
        overview.put("recentExamTrend", trend);

        return ResponseResult.success(overview);
    }
}
