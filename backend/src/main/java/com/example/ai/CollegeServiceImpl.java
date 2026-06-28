package com.example.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.CollegeBasic;
import com.example.mapper.CollegeBasicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollegeServiceImpl implements CollegeService {

    private final CollegeBasicMapper collegeBasicMapper;

    @Override
    public boolean refreshColleges() {
        List<CollegeBasic> seeds = Arrays.asList(
                seed("北京大学", "985"),
                seed("清华大学", "985"),
                seed("郑州大学", "211"),
                seed("河南大学", "first_class"),
                seed("洛阳师范学院", "second_class")
        );
        for (CollegeBasic seed : seeds) {
            CollegeBasic existing = collegeBasicMapper.selectOne(new LambdaQueryWrapper<CollegeBasic>()
                    .eq(CollegeBasic::getCollegeName, seed.getCollegeName())
                    .last("LIMIT 1"));
            if (existing == null) {
                collegeBasicMapper.insert(seed);
            }
        }
        return true;
    }

    @Override
    public List<CollegeBasic> getRandomCollegesByBatch(String admissionBatch, String subjectType, int limit) {
        List<CollegeBasic> colleges = collegeBasicMapper.selectList(new LambdaQueryWrapper<CollegeBasic>()
                .eq(CollegeBasic::getAdmissionBatch, admissionBatch)
                .and(subjectType != null && !subjectType.isEmpty(),
                        wrapper -> wrapper.eq(CollegeBasic::getSubjectType, subjectType).or().eq(CollegeBasic::getSubjectType, "both")));
        Collections.shuffle(colleges);
        return colleges.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<String> getBatchList() {
        return Arrays.asList("985", "211", "first_class", "second_class");
    }

    @Override
    public List<CollegeBasic> searchCollege(String keyword) {
        return collegeBasicMapper.selectList(new LambdaQueryWrapper<CollegeBasic>()
                .like(keyword != null && !keyword.isEmpty(), CollegeBasic::getCollegeName, keyword));
    }

    @Override
    public CollegeBasic getDreamCollege(String collegeName) {
        if (collegeName == null || collegeName.isEmpty()) return null;
        return collegeBasicMapper.selectOne(new LambdaQueryWrapper<CollegeBasic>()
                .eq(CollegeBasic::getCollegeName, collegeName)
                .last("LIMIT 1"));
    }

    private CollegeBasic seed(String name, String batch) {
        CollegeBasic college = new CollegeBasic();
        college.setCollegeName(name);
        college.setAdmissionBatch(batch);
        college.setSubjectType("both");
        college.setLogoPath("/logos/default.svg");
        college.setLastCrawled(new Date());
        college.setProvince("河南");
        college.setYear(Calendar.getInstance().get(Calendar.YEAR));
        // 设置稳妥位次范围
        switch (batch) {
            case "985":
                college.setMinRank(500);
                college.setMaxRank(12000);
                break;
            case "211":
                college.setMinRank(10000);
                college.setMaxRank(58000);
                break;
            case "first_class":
                college.setMinRank(40000);
                college.setMaxRank(95000);
                break;
            case "second_class":
                college.setMinRank(100000);
                college.setMaxRank(200000);
                break;
        }
        return college;
    }
}
