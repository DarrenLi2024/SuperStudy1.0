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
        overview.put("recentExamTrend", growthData.getScoreTrend());
        overview.put("rankTrend", growthData.getRankTrend());
        overview.put("examRecords", examRecords);

        return ResponseResult.success(overview);
    }
}
