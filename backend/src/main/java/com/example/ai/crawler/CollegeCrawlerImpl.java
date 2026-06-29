package com.example.ai.crawler;

import com.example.ai.client.LlmClient;
import com.example.ai.client.LlmRequest;
import com.example.ai.client.LlmResponse;
import com.example.entity.CollegeBasic;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 院校数据爬虫实现
 * 通过LLM智能获取中国高校录取信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollegeCrawlerImpl implements CollegeCrawler {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /** 中国高校批次分类 */
    private static final Map<String, String> BATCH_LABELS = new LinkedHashMap<>();
    static {
        BATCH_LABELS.put("985", "985工程高校（39所）");
        BATCH_LABELS.put("211", "211工程高校（含985，共112所）");
        BATCH_LABELS.put("first_class", "双一流建设高校");
        BATCH_LABELS.put("second_class", "普通本科院校");
    }

    /** 各批次典型录取位次范围（理科/物理类，河南省参考） */
    private static final Map<String, int[]> BATCH_RANK_RANGE = new LinkedHashMap<>();
    static {
        BATCH_RANK_RANGE.put("985", new int[]{500, 15000});
        BATCH_RANK_RANGE.put("211", new int[]{8000, 60000});
        BATCH_RANK_RANGE.put("first_class", new int[]{35000, 110000});
        BATCH_RANK_RANGE.put("second_class", new int[]{90000, 250000});
    }

    @Override
    public List<CollegeBasic> crawlCollegesByBatch(String batch) {
        String batchLabel = BATCH_LABELS.getOrDefault(batch, batch);
        log.info("开始抓取院校数据: {} ({})", batch, batchLabel);

        int[] rankRange = BATCH_RANK_RANGE.getOrDefault(batch, new int[]{50000, 150000});

        LlmRequest request = LlmRequest.builder()
                .taskType("college_crawl")
                .systemPrompt(buildSystemPrompt())
                .userPrompt(buildCrawlPrompt(batch, batchLabel, rankRange))
                .responseSchema(buildResponseSchema())
                .temperature(0.1)
                .maxTokens(4096)
                .skipCache(true)
                .build();

        try {
            LlmResponse response = llmClient.generate(request);
            if (response.isFallback() || response.getContent() == null || "{}".equals(response.getContent())) {
                log.warn("LLM院校抓取失败，使用种子数据: {}", batch);
                return getSeedColleges(batch);
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
                log.warn("LLM返回空院校数据: {}", batch);
                return getSeedColleges(batch);
            }

            List<CollegeBasic> colleges = items.stream()
                    .map(item -> toCollegeBasic(item, batch, rankRange))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("成功抓取院校数据: {} 共{}所", batch, colleges.size());
            return colleges;

        } catch (Exception e) {
            log.error("院校抓取异常: {} - {}", batch, e.getMessage());
            return getSeedColleges(batch);
        }
    }

    @Override
    public String crawlCollegeLogo(String collegeName) {
        // 院校LOGO暂时使用默认路径，后续可对接图标CDN
        return "/logos/default.svg";
    }

    // ==================== Prompt构建 ====================

    private String buildSystemPrompt() {
        return """
                你是一位中国高等教育数据专家。你需要提供中国高校的准确信息。

                ## 数据规则
                1. 院校名称必须为教育部认证的正式全称
                2. 省份为院校主校区所在地
                3. 录取位次范围为该批次院校在河南省物理类近年的典型录取位次区间
                4. 输出严格JSON数组格式

                ## 安全约束
                - 只输出客观的院校信息
                - 不得包含任何政治敏感内容
                - 不得对院校进行排名或优劣评价
                """;
    }

    private String buildCrawlPrompt(String batch, String batchLabel, int[] rankRange) {
        return String.format("""
                请列出%s的典型院校信息（至少8所）。

                批次说明：%s
                典型录取位次范围：全省%d-%d名（物理类，河南省参考）

                请确保覆盖不同省份和不同类型的院校。

                请输出JSON数组，每条包含：
                - collegeName: 院校全称
                - province: 所在省份
                - admissionBatch: 批次标识(%s)
                - subjectType: 招生科类(physics/history/both)
                - minRank: 最低录取位次
                - maxRank: 最高录取位次
                - year: 数据年份
                """, batchLabel, batchLabel, rankRange[0], rankRange[1], batch);
    }

    private String buildResponseSchema() {
        return """
                [
                  {
                    "collegeName": "北京大学",
                    "province": "北京",
                    "admissionBatch": "985",
                    "subjectType": "both",
                    "minRank": 500,
                    "maxRank": 8000,
                    "year": 2025
                  }
                ]""";
    }

    // ==================== 数据转换 ====================

    private CollegeBasic toCollegeBasic(Map<String, Object> item, String batch, int[] rankRange) {
        try {
            CollegeBasic college = new CollegeBasic();
            college.setCollegeName(String.valueOf(item.getOrDefault("collegeName", "")));
            college.setProvince(String.valueOf(item.getOrDefault("province", "")));
            college.setAdmissionBatch(String.valueOf(item.getOrDefault("admissionBatch", batch)));
            college.setSubjectType(String.valueOf(item.getOrDefault("subjectType", "both")));
            college.setMinRank(toInt(item.get("minRank"), rankRange[0]));
            college.setMaxRank(toInt(item.get("maxRank"), rankRange[1]));
            college.setYear(toInt(item.get("year"), Calendar.getInstance().get(Calendar.YEAR)));
            college.setLogoPath("/logos/default.svg");
            college.setLastCrawled(new Date());

            if (college.getCollegeName() == null || college.getCollegeName().trim().isEmpty()) {
                return null;
            }
            return college;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // ==================== 种子数据降级 ====================

    private List<CollegeBasic> getSeedColleges(String batch) {
        Map<String, List<String[]>> seedMap = new LinkedHashMap<>();
        seedMap.put("985", Arrays.asList(
                new String[]{"北京大学", "北京", "both", "500", "8000"},
                new String[]{"清华大学", "北京", "both", "400", "6000"},
                new String[]{"复旦大学", "上海", "both", "800", "10000"},
                new String[]{"上海交通大学", "上海", "both", "600", "9000"},
                new String[]{"浙江大学", "浙江", "both", "1000", "12000"},
                new String[]{"南京大学", "江苏", "both", "1200", "14000"},
                new String[]{"中国科学技术大学", "安徽", "physics", "800", "10000"},
                new String[]{"武汉大学", "湖北", "both", "2000", "15000"},
                new String[]{"华中科技大学", "湖北", "both", "2500", "16000"},
                new String[]{"中山大学", "广东", "both", "3000", "18000"}
        ));
        seedMap.put("211", Arrays.asList(
                new String[]{"郑州大学", "河南", "both", "10000", "45000"},
                new String[]{"南昌大学", "江西", "both", "15000", "50000"},
                new String[]{"合肥工业大学", "安徽", "physics", "12000", "40000"},
                new String[]{"武汉理工大学", "湖北", "both", "10000", "38000"},
                new String[]{"西南交通大学", "四川", "both", "12000", "42000"},
                new String[]{"南京航空航天大学", "江苏", "physics", "8000", "35000"},
                new String[]{"北京科技大学", "北京", "both", "8000", "30000"},
                new String[]{"华东理工大学", "上海", "both", "6000", "28000"}
        ));
        seedMap.put("first_class", Arrays.asList(
                new String[]{"河南大学", "河南", "both", "40000", "90000"},
                new String[]{"河南师范大学", "河南", "both", "50000", "100000"},
                new String[]{"河南科技大学", "河南", "both", "45000", "95000"},
                new String[]{"河南理工大学", "河南", "physics", "48000", "100000"},
                new String[]{"河南工业大学", "河南", "both", "50000", "105000"},
                new String[]{"河南财经政法大学", "河南", "both", "45000", "95000"}
        ));
        seedMap.put("second_class", Arrays.asList(
                new String[]{"洛阳师范学院", "河南", "both", "100000", "200000"},
                new String[]{"南阳师范学院", "河南", "both", "105000", "210000"},
                new String[]{"安阳师范学院", "河南", "both", "110000", "220000"},
                new String[]{"周口师范学院", "河南", "both", "115000", "230000"},
                new String[]{"商丘师范学院", "河南", "both", "110000", "225000"}
        ));

        List<String[]> seeds = seedMap.getOrDefault(batch, seedMap.get("first_class"));
        int year = Calendar.getInstance().get(Calendar.YEAR);

        return seeds.stream().map(s -> {
            CollegeBasic college = new CollegeBasic();
            college.setCollegeName(s[0]);
            college.setProvince(s[1]);
            college.setAdmissionBatch(batch);
            college.setSubjectType(s[2]);
            college.setMinRank(Integer.parseInt(s[3]));
            college.setMaxRank(Integer.parseInt(s[4]));
            college.setLogoPath("/logos/default.svg");
            college.setLastCrawled(new Date());
            college.setYear(year);
            return college;
        }).collect(Collectors.toList());
    }
}
