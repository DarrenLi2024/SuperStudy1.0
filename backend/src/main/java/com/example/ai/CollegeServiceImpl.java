package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ai.crawler.CollegeDataProvider;
import com.example.entity.CollegeBasic;
import com.example.mapper.CollegeBasicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollegeServiceImpl implements CollegeService {

    private final CollegeBasicMapper collegeBasicMapper;
    private final CollegeDataProvider collegeDataProvider;

    /** 所有可用批次 */
    private static final List<String> ALL_BATCHES = Arrays.asList("985", "211", "first_class", "second_class");

    @Override
    public boolean refreshColleges() {
        int totalInserted = 0;
        for (String batch : ALL_BATCHES) {
            try {
                List<CollegeBasic> colleges = collegeDataProvider.getCollegesByBatch(batch);
                int inserted = saveColleges(colleges);
                totalInserted += inserted;
                log.info("院校数据刷新完成: {} 插入{}所", batch, inserted);
            } catch (Exception e) {
                log.error("院校数据刷新失败: {} - {}", batch, e.getMessage());
            }
        }
        log.info("院校数据刷新完成，共插入{}所", totalInserted);
        return true;
    }

    @Override
    public List<CollegeBasic> getRandomCollegesByBatch(String admissionBatch, String subjectType, int limit) {
        List<CollegeBasic> colleges = collegeBasicMapper.selectList(new LambdaQueryWrapper<CollegeBasic>()
                .eq(CollegeBasic::getAdmissionBatch, admissionBatch)
                .and(subjectType != null && !subjectType.isEmpty(),
                        wrapper -> wrapper.eq(CollegeBasic::getSubjectType, subjectType)
                                .or().eq(CollegeBasic::getSubjectType, "both")));

        // 如果数据库数据不足，触发一次抓取
        if (colleges.size() < limit) {
            try {
                List<CollegeBasic> fresh = collegeDataProvider.getCollegesByBatch(admissionBatch);
                saveColleges(fresh);
                // 重新查询
                colleges = collegeBasicMapper.selectList(new LambdaQueryWrapper<CollegeBasic>()
                        .eq(CollegeBasic::getAdmissionBatch, admissionBatch)
                        .and(subjectType != null && !subjectType.isEmpty(),
                                wrapper -> wrapper.eq(CollegeBasic::getSubjectType, subjectType)
                                        .or().eq(CollegeBasic::getSubjectType, "both")));
            } catch (Exception e) {
                log.warn("院校数据不足，自动抓取失败: {} - {}", admissionBatch, e.getMessage());
            }
        }

        Collections.shuffle(colleges);
        return colleges.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<String> getBatchList() {
        return ALL_BATCHES;
    }

    @Override
    public List<CollegeBasic> searchCollege(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return collegeBasicMapper.selectList(new LambdaQueryWrapper<CollegeBasic>()
                .like(CollegeBasic::getCollegeName, keyword.trim()));
    }

    @Override
    public CollegeBasic getDreamCollege(String collegeName) {
        if (collegeName == null || collegeName.isEmpty()) return null;
        return collegeBasicMapper.selectOne(new LambdaQueryWrapper<CollegeBasic>()
                .eq(CollegeBasic::getCollegeName, collegeName)
                .last("LIMIT 1"));
    }

    // ==================== 内部方法 ====================

    private int saveColleges(List<CollegeBasic> colleges) {
        int inserted = 0;
        for (CollegeBasic college : colleges) {
            // 检查是否已存在
            CollegeBasic existing = collegeBasicMapper.selectOne(new LambdaQueryWrapper<CollegeBasic>()
                    .eq(CollegeBasic::getCollegeName, college.getCollegeName())
                    .last("LIMIT 1"));
            if (existing == null && college.getCollegeName() != null) {
                college.setLastCrawled(new Date());
                inserted += collegeBasicMapper.insert(college);
            }
        }
        return inserted;
    }
}
