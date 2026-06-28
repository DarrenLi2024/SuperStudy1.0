package com.example.growth;

import com.example.dto.response.CollegeCardResponse;
import com.example.dto.response.GrowthDataResponse;
import com.example.dto.response.GrowthProgressResponse;
import com.example.growth.entity.GrowthRecord;
import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/growth")
public class GrowthController {

    @Autowired
    private GrowthService growthService;

    /**
     * 获取三段式院校卡片数据
     */
    @GetMapping("/cards/{studentId}")
    public ResponseResult<CollegeCardResponse> getCollegeCards(@PathVariable Long studentId) {
        CollegeCardResponse cards = growthService.getCollegeCards(studentId);
        return ResponseResult.success(cards);
    }

    /**
     * 获取段位进度条数据
     */
    @GetMapping("/progress/{studentId}")
    public ResponseResult<GrowthProgressResponse> getGrowthProgress(@PathVariable Long studentId) {
        GrowthProgressResponse progress = growthService.getGrowthProgress(studentId);
        return ResponseResult.success(progress);
    }

    /**
     * 获取段位升级历史记录
     */
    @GetMapping("/history/{studentId}")
    public ResponseResult<List<GrowthRecord>> getGrowthHistory(@PathVariable Long studentId) {
        List<GrowthRecord> history = growthService.getGrowthHistory(studentId);
        return ResponseResult.success(history);
    }

    /**
     * 获取成长数据（总分趋势、位次曲线、单科曲线）
     */
    @GetMapping("/data/{studentId}")
    public ResponseResult<GrowthDataResponse> getGrowthData(@PathVariable Long studentId) {
        GrowthDataResponse data = growthService.getGrowthData(studentId);
        return ResponseResult.success(data);
    }
}
