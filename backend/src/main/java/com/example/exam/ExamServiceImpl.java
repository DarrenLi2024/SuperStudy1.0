package com.example.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.config.BusinessException;
import com.example.dto.request.ExamSubmitRequest;
import com.example.dto.response.*;
import com.example.entity.CollegeBasic;
import com.example.entity.ScoreRank;
import com.example.exam.entity.ExamRecord;
import com.example.exam.mapper.ExamRecordMapper;
import com.example.mapper.CollegeBasicMapper;
import com.example.mapper.ScoreRankMapper;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    @Autowired
    private ScoreRankMapper scoreRankMapper;

    @Autowired
    private CollegeBasicMapper collegeBasicMapper;

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Override
    public ExamRecord submitExam(Long studentId, ExamSubmitRequest request) {
        // 计算总分
        int totalScore = request.getSubjectScores().values().stream().mapToInt(Integer::intValue).sum();

        // 简单折算：将校内分数映射为等效高考分（按0.85系数加权，后续可由AI优化）
        int equivalentGaokaoScore = (int) Math.round(totalScore * 0.85);

        // 根据等效分匹配位次
        Integer equivalentRank = findRankByScore(equivalentGaokaoScore);

        // 计算当前批次
        String currentBatch = calculateBatch(equivalentGaokaoScore);

        // 构建JSON格式的各科分数
        StringBuilder subjectScoresJson = new StringBuilder("{");
        for (Map.Entry<String, Integer> entry : request.getSubjectScores().entrySet()) {
            subjectScoresJson.append("\"").append(entry.getKey()).append("\":").append(entry.getValue()).append(",");
        }
        if (subjectScoresJson.length() > 1) {
            subjectScoresJson.deleteCharAt(subjectScoresJson.length() - 1);
        }
        subjectScoresJson.append("}");

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setExamType(request.getExamType());
        record.setSubjectScores(subjectScoresJson.toString());
        record.setTotalScore(totalScore);
        record.setEquivalentGaokaoScore(equivalentGaokaoScore);
        record.setEquivalentRank(equivalentRank);
        record.setCurrentBatch(currentBatch);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            record.setExamDate(sdf.parse(request.getExamDate()));
        } catch (Exception e) {
            record.setExamDate(new Date());
        }

        // AI诊断报告（简化版，后续接入真实LLM）
        String diagnosis = generateDiagnosis(request.getSubjectScores(), batchDisplayName(currentBatch));
        record.setAiDiagnosisReport(diagnosis);

        this.save(record);
        return record;
    }

    @Override
    public List<ExamRecordResponse> getExamRecords(Long studentId) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getExamDate);
        List<ExamRecord> records = this.list(queryWrapper);
        return records.stream().map(ExamRecordResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    public ExamRecordResponse getExamDetail(Long examId) {
        ExamRecord record = this.getById(examId);
        if (record == null) {
            throw new BusinessException(404, "考试记录不存在");
        }
        return ExamRecordResponse.fromEntity(record);
    }

    @Override
    public String calculateBatch(Integer score) {
        if (score == null) return "unknown";
        if (score >= 600) return "985";
        if (score >= 550) return "211";
        if (score >= 480) return "first_class";
        if (score >= 400) return "second_class";
        return "below_本科";
    }

    /**
     * 将批次编码转换为中文显示名（供前端使用）
     */
    public static String batchDisplayName(String code) {
        if (code == null) return "未知";
        switch (code) {
            case "985": return "985/双一流";
            case "211": return "211";
            case "first_class": return "普通一本";
            case "second_class": return "公办二本";
            case "below_本科": return "本科以下";
            default: return code;
        }
    }

    @Override
    public CollegeCardResponse getCollegeCards(Long studentId) {
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }

        Integer currentScore = profile.getBaselineScore() != null ? profile.getBaselineScore() : 450;
        String currentBatch = calculateBatch(currentScore);
        String targetBatch = profile.getTargetScore() != null ?
                calculateBatch(profile.getTargetScore()) : "211/双一流";

        // 获取当前批次院校
        List<CollegeCardResponse.CollegeInfo> currentColleges = getRandomColleges(currentBatch);

        // 获取目标批次院校
        List<CollegeCardResponse.CollegeInfo> targetColleges = getRandomColleges(targetBatch);

        // 心仪院校信息
        CollegeCardResponse.DreamCollegeInfo dreamInfo = buildDreamCollegeInfo(profile, currentScore);

        return CollegeCardResponse.builder()
                .currentBatchCards(currentColleges)
                .targetBatchCards(targetColleges)
                .dreamCollege(dreamInfo)
                .currentBatch(batchDisplayName(currentBatch))
                .currentScore(currentScore)
                .build();
    }

    @Override
    public GrowthProgressResponse getGrowthProgress(Long studentId) {
        StudentProfile profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "学生档案不存在");
        }

        Integer currentScore = profile.getBaselineScore() != null ? profile.getBaselineScore() : 450;
        Integer targetScore = profile.getTargetScore() != null ? profile.getTargetScore() : 600;

        // 获取最新的考试记录，更新当前分数
        LambdaQueryWrapper<ExamRecord> examQuery = new LambdaQueryWrapper<>();
        examQuery.eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getExamDate)
                .last("LIMIT 1");
        ExamRecord latestExam = this.getOne(examQuery);
        if (latestExam != null && latestExam.getEquivalentGaokaoScore() != null) {
            currentScore = latestExam.getEquivalentGaokaoScore();
        }

        // 定义三个段位阶段
        List<GrowthProgressResponse.Phase> phases = new ArrayList<>();

        // 阶段1: 二本阶段 (400-480)
        phases.add(buildPhase("二本阶段", 400, 480, currentScore));
        // 阶段2: 一本阶段 (480-550)
        phases.add(buildPhase("一本阶段", 480, 550, currentScore));
        // 阶段3: 985/211阶段 (550-650)
        phases.add(buildPhase("985/211阶段", 550, 650, currentScore));

        // 计算总进度
        double totalProgress = calculateTotalProgress(currentScore, 400, 650);

        return GrowthProgressResponse.builder()
                .phases(phases)
                .totalProgress(totalProgress)
                .targetScore(targetScore)
                .build();
    }

    @Override
    public GrowthDataResponse getGrowthData(Long studentId) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getStudentId, studentId)
                .orderByAsc(ExamRecord::getExamDate);
        List<ExamRecord> records = this.list(queryWrapper);

        List<GrowthDataResponse.ScoreTrend> scoreTrends = new ArrayList<>();
        List<GrowthDataResponse.RankTrend> rankTrends = new ArrayList<>();
        Map<String, List<GrowthDataResponse.SubjectTrend>> subjectTrends = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (ExamRecord record : records) {
            String dateStr = sdf.format(record.getExamDate());

            // 总分趋势
            if (record.getEquivalentGaokaoScore() != null) {
                scoreTrends.add(GrowthDataResponse.ScoreTrend.builder()
                        .date(dateStr).score(record.getEquivalentGaokaoScore()).build());
            }

            // 位次趋势
            if (record.getEquivalentRank() != null) {
                rankTrends.add(GrowthDataResponse.RankTrend.builder()
                        .date(dateStr).rank(record.getEquivalentRank()).build());
            }

            // 单科趋势
            if (record.getSubjectScores() != null) {
                String scores = record.getSubjectScores();
                // 简单解析JSON格式的各科分数
                String[] parts = scores.replace("{", "").replace("}", "").split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        String subject = kv[0].replace("\"", "").trim();
                        try {
                            int score = Integer.parseInt(kv[1].trim());
                            subjectTrends.computeIfAbsent(subject, k -> new ArrayList<>());
                            subjectTrends.get(subject).add(GrowthDataResponse.SubjectTrend.builder()
                                    .date(dateStr).score(score).build());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        return GrowthDataResponse.builder()
                .scoreTrend(scoreTrends)
                .rankTrend(rankTrends)
                .subjectTrends(subjectTrends)
                .monthlyReport("AI月报将在后续版本中自动生成")
                .build();
    }

    // ==================== 私有辅助方法 ====================

    private Integer findRankByScore(Integer score) {
        if (score == null) return null;
        LambdaQueryWrapper<ScoreRank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScoreRank::getScore, score)
                .eq(ScoreRank::getYear, Calendar.getInstance().get(Calendar.YEAR))
                .last("LIMIT 1");
        ScoreRank rank = scoreRankMapper.selectOne(queryWrapper);
        return rank != null ? rank.getRankValue() : estimateRank(score);
    }

    private Integer estimateRank(Integer score) {
        // 简单估算：若没有精确匹配，找最近的分数
        LambdaQueryWrapper<ScoreRank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(ScoreRank::getScore, score + 10)
                .ge(ScoreRank::getScore, score - 10)
                .last("LIMIT 1");
        ScoreRank rank = scoreRankMapper.selectOne(queryWrapper);
        return rank != null ? rank.getRankValue() : score * 100; // 粗略估算
    }

    private List<CollegeCardResponse.CollegeInfo> getRandomColleges(String batch) {
        if (batch == null) return new ArrayList<>();
        // 使用 LambdaQueryWrapper 替代 XML Mapper（避免 XML 映射问题）
        LambdaQueryWrapper<CollegeBasic> wrapper = new LambdaQueryWrapper<CollegeBasic>()
                .eq(CollegeBasic::getAdmissionBatch, batch);
        List<CollegeBasic> all = collegeBasicMapper.selectList(wrapper);
        Collections.shuffle(all);
        List<CollegeBasic> colleges = all.stream().limit(3).collect(Collectors.toList());
        return colleges.stream().map(c -> CollegeCardResponse.CollegeInfo.builder()
                .id(c.getId())
                .name(c.getCollegeName())
                .logo(c.getLogoPath() != null ? c.getLogoPath() : "/logos/default.svg")
                .batch(batchDisplayName(c.getAdmissionBatch()))
                .build()).collect(Collectors.toList());
    }

    private CollegeCardResponse.DreamCollegeInfo buildDreamCollegeInfo(StudentProfile profile, Integer currentScore) {
        Integer targetScore = profile.getTargetScore() != null ? profile.getTargetScore() : 600;
        int scoreGap = Math.max(0, targetScore - currentScore);

        // 模拟各科缺口
        List<CollegeCardResponse.SubjectGap> subjectGaps = Arrays.asList(
                CollegeCardResponse.SubjectGap.builder().subject("数学").gap((int) (scoreGap * 0.4)).build(),
                CollegeCardResponse.SubjectGap.builder().subject("英语").gap((int) (scoreGap * 0.3)).build(),
                CollegeCardResponse.SubjectGap.builder().subject("综合").gap((int) (scoreGap * 0.3)).build()
        );

        // 生成激励文案
        String incentive = generateIncentive(currentScore, targetScore, scoreGap);

        return CollegeCardResponse.DreamCollegeInfo.builder()
                .name(profile.getDreamCollege() != null ? profile.getDreamCollege() : "目标院校")
                .logo("/logos/dream.png")
                .batch(profile.getDreamCollegeBatch() != null ? batchDisplayName(profile.getDreamCollegeBatch()) : (targetScore >= 550 ? "985/双一流" : "普通一本"))
                .scoreGap(scoreGap)
                .subjectGaps(subjectGaps)
                .aiIncentive(incentive)
                .build();
    }

    private GrowthProgressResponse.Phase buildPhase(String name, int startScore, int endScore, int currentScore) {
        boolean completed = currentScore >= endScore;
        double progress;
        if (currentScore <= startScore) {
            progress = 0;
        } else if (completed) {
            progress = 100;
        } else {
            progress = (double) (currentScore - startScore) / (endScore - startScore) * 100;
        }
        return GrowthProgressResponse.Phase.builder()
                .name(name)
                .startScore(startScore)
                .endScore(endScore)
                .currentScore(currentScore)
                .progress(progress)
                .completed(completed)
                .build();
    }

    private double calculateTotalProgress(int currentScore, int minScore, int maxScore) {
        if (currentScore <= minScore) return 0;
        if (currentScore >= maxScore) return 100;
        return (double) (currentScore - minScore) / (maxScore - minScore) * 100;
    }

    private String generateDiagnosis(Map<String, Integer> subjectScores, String batch) {
        if (subjectScores == null || subjectScores.isEmpty()) {
            return "本次考试整体表现正常，继续保持当前学习节奏。";
        }

        // 找出最弱和最擅长的科目
        Optional<Map.Entry<String, Integer>> minEntry = subjectScores.entrySet().stream()
                .min(Map.Entry.comparingByValue());
        Optional<Map.Entry<String, Integer>> maxEntry = subjectScores.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        StringBuilder sb = new StringBuilder();
        sb.append("【AI诊断报告】\n");
        sb.append("当前稳定批次：").append(batch).append("\n");

        minEntry.ifPresent(entry ->
                sb.append("薄弱科目：").append(entry.getKey()).append("（").append(entry.getValue()).append("分），建议加强针对性训练。\n"));

        maxEntry.ifPresent(entry ->
                sb.append("优势科目：").append(entry.getKey()).append("（").append(entry.getValue()).append("分），继续保持。\n"));

        sb.append("整体分析：本次考试全面检验了近期学习成果，建议针对薄弱环节进行专项突破。");

        return sb.toString();
    }

    private String generateIncentive(int currentScore, int targetScore, int gap) {
        if (gap <= 0) {
            return "太棒了！你已经达到目标分数，继续保持冲击更高层次！";
        }
        if (gap <= 30) {
            return "距离目标仅剩" + gap + "分！你的努力正在见效，坚持下去一定能突破！";
        }
        if (gap <= 80) {
            return "还差" + gap + "分就能达成目标！每天进步一点点，梦想就在前方！";
        }
        return "目标差距" + gap + "分，但每一个高分考生都从当下开始。专注每一天，进步看得见！";
    }
}
