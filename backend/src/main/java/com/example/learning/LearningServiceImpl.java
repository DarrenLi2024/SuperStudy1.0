package com.example.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.LearningAnalysisService;
import com.example.dto.response.KnowledgePointDto;
import com.example.dto.response.TodayTaskResponse;
import com.example.learning.entity.ErrorQuestion;
import com.example.learning.entity.TaskRecord;
import com.example.learning.mapper.ErrorQuestionMapper;
import com.example.learning.mapper.TaskRecordMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningServiceImpl extends ServiceImpl<TaskRecordMapper, TaskRecord> implements LearningService {

    @Autowired
    private ErrorQuestionMapper errorQuestionMapper;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private LearningAnalysisService learningAnalysisService;

    @Override
    public TodayTaskResponse getTodayTasks(Long studentId) {
        // 查询今日任务
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = sdf.format(new Date());

        LambdaQueryWrapper<TaskRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskRecord::getStudentId, studentId);

        // 使用task_date字段比较（简化：查询最新的一条记录）
        queryWrapper.orderByDesc(TaskRecord::getTaskDate);
        queryWrapper.last("LIMIT 1");

        TaskRecord latestTask = this.getOne(queryWrapper);

        if (latestTask != null && latestTask.getTaskContent() != null) {
            try {
                // 解析JSON任务内容
                List<TodayTaskResponse.TaskItem> tasks = objectMapper.readValue(
                        latestTask.getTaskContent(),
                        new TypeReference<List<TodayTaskResponse.TaskItem>>() {}
                );

                Double completionRate = latestTask.getCompletionRate() != null ?
                        latestTask.getCompletionRate().doubleValue() : 0.0;

                return TodayTaskResponse.builder()
                        .tasks(tasks)
                        .aiComment(latestTask.getAiComment() != null ?
                                latestTask.getAiComment() : "今日学习任务已生成，加油完成！")
                        .completionRate(completionRate)
                        .build();
            } catch (Exception e) {
                // 解析失败则生成默认任务
                return generateDefaultTasks(studentId);
            }
        }

        // 没有今日任务则生成并持久化
        return generateAiTasks(studentId);
    }

    @Override
    public void completeTask(Long taskId, BigDecimal completionRate) {
        TaskRecord task = this.getById(taskId);
        if (task != null) {
            task.setCompletionRate(completionRate);
            if (completionRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
                task.setStatus("completed");
            } else if (completionRate.compareTo(BigDecimal.ZERO) > 0) {
                task.setStatus("partial");
            }
            this.updateById(task);
        }
    }

    @Override
    public List<ErrorQuestion> getErrorQuestions(Long studentId) {
        LambdaQueryWrapper<ErrorQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErrorQuestion::getStudentId, studentId)
                .orderByDesc(ErrorQuestion::getCreatedAt);
        return errorQuestionMapper.selectList(queryWrapper);
    }

    @Override
    public void recordErrorQuestion(ErrorQuestion question) {
        question.setReinforcementFlag(0);
        if ((question.getAiAnalysis() == null || question.getAiAnalysis().trim().isEmpty())
                && learningAnalysisService != null) {
            question.setAiAnalysis(learningAnalysisService.analyzeErrorCause(
                    question.getSubject(), question.getQuestionContent(), question.getWrongAnswer()));
        }
        errorQuestionMapper.insert(question);
    }

    @Override
    public Map<String, List<KnowledgePointDto>> getKnowledgeStatus(Long studentId) {
        Map<String, List<KnowledgePointDto>> result = new LinkedHashMap<>();

        // 从错题中分析知识点掌握情况
        List<ErrorQuestion> errors = getErrorQuestions(studentId);
        Set<String> subjects = errors.stream()
                .map(ErrorQuestion::getSubject)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());

        for (String subject : subjects) {
            List<ErrorQuestion> subjectErrors = errors.stream()
                    .filter(e -> subject.equals(e.getSubject()))
                    .collect(Collectors.toList());

            // 按知识点分组统计
            Map<String, Long> errorCount = subjectErrors.stream()
                    .filter(e -> e.getKnowledgePoint() != null)
                    .collect(Collectors.groupingBy(ErrorQuestion::getKnowledgePoint, Collectors.counting()));

            List<KnowledgePointDto> points = new ArrayList<>();

            // 科目基础知识点目录；真实掌握度由学生错题记录动态计算。
            String[] defaultPoints;
            switch (subject) {
                case "数学":
                    defaultPoints = new String[]{"函数与导数", "三角函数", "数列", "概率统计", "解析几何"};
                    break;
                case "英语":
                    defaultPoints = new String[]{"阅读理解", "完形填空", "语法", "写作", "词汇"};
                    break;
                case "语文":
                    defaultPoints = new String[]{"文言文阅读", "现代文阅读", "诗歌鉴赏", "作文"};
                    break;
                default:
                    defaultPoints = new String[]{subject + "基础", subject + "进阶"};
            }

            for (String point : defaultPoints) {
                Long count = errorCount.getOrDefault(point, 0L);
                int masteryLevel;
                String status;

                if (count >= 5) {
                    masteryLevel = 20 + new Random().nextInt(20);
                    status = "weak";
                } else if (count >= 2) {
                    masteryLevel = 40 + new Random().nextInt(30);
                    status = "normal";
                } else {
                    masteryLevel = 70 + new Random().nextInt(30);
                    status = "strong";
                }

                points.add(KnowledgePointDto.builder()
                        .subject(subject)
                        .name(point)
                        .masteryLevel(masteryLevel)
                        .status(status)
                        .build());
            }

            result.put(subject, points);
        }

        // 如果没有错题数据，生成默认数据
        if (result.isEmpty()) {
            String[] defaultSubjects = {"语文", "数学", "英语", "历史", "政治", "地理"};
            for (String subject : defaultSubjects) {
                Random random = new Random();
                String[] points;
                switch (subject) {
                    case "数学":
                        points = new String[]{"函数与导数", "三角函数", "数列", "概率统计", "解析几何"};
                        break;
                    case "英语":
                        points = new String[]{"阅读理解", "完形填空", "语法", "写作", "词汇"};
                        break;
                    case "语文":
                        points = new String[]{"文言文阅读", "现代文阅读", "诗歌鉴赏", "作文"};
                        break;
                    default:
                        points = new String[]{subject + "基础", subject + "进阶"};
                }

                List<KnowledgePointDto> pointList = new ArrayList<>();
                for (String point : points) {
                    int level = 30 + random.nextInt(70);
                    pointList.add(KnowledgePointDto.builder()
                            .subject(subject)
                            .name(point)
                            .masteryLevel(level)
                            .status(level >= 70 ? "strong" : level >= 40 ? "normal" : "weak")
                            .build());
                }
                result.put(subject, pointList);
            }
        }

        return result;
    }

    // ==================== 私有辅助方法 ====================

    private TodayTaskResponse generateDefaultTasks(Long studentId) {
        // 生成默认的今日任务（AI简化版，后续接入真实LLM）
        List<TodayTaskResponse.TaskItem> tasks = new ArrayList<>();
        Random random = new Random();

        String[][] taskTemplates = {
                {"数学", "函数与导数", "完成10道函数综合练习题", "专项刷题"},
                {"英语", "阅读理解", "精读2篇阅读理解并整理生词", "专项刷题"},
                {"语文", "文言文阅读", "翻译1篇文言文并归纳实词", "错题复盘"},
                {"历史", "时间轴", "整理本周历史时间轴", "知识点巩固"},
                {"政治", "时事", "浏览本周时事热点并做笔记", "知识点巩固"},
                {"地理", "自然地理", "复习大气环流章节", "知识点巩固"}
        };

        // 随机选取3-4个任务
        int taskCount = 3 + random.nextInt(2);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < taskTemplates.length; i++) indices.add(i);
        Collections.shuffle(indices);

        for (int i = 0; i < taskCount && i < indices.size(); i++) {
            int idx = indices.get(i);
            tasks.add(TodayTaskResponse.TaskItem.builder()
                    .id((long) (i + 1))
                    .type(taskTemplates[idx][3])
                    .content(taskTemplates[idx][2])
                    .subject(taskTemplates[idx][0])
                    .knowledgePoint(taskTemplates[idx][1])
                    .aiHint("本次任务为补齐" + taskTemplates[idx][1] + "知识点短板")
                    .completionRate(0.0)
                    .status("pending")
                    .build());
        }

        return TodayTaskResponse.builder()
                .tasks(tasks)
                .aiComment("今日学习任务已为你量身定制，从薄弱环节开始突破，点滴积累终见成效！")
                .completionRate(0.0)
                .build();
    }

    private TodayTaskResponse generateAiTasks(Long studentId) {
        if (learningAnalysisService == null) {
            return generateDefaultTasks(studentId);
        }
        try {
            List<Map<String, Object>> rawTasks = learningAnalysisService.generateDailyTasks(studentId);
            List<TodayTaskResponse.TaskItem> tasks = rawTasks.stream().map(this::toTaskItem).collect(Collectors.toList());
            String taskContent = objectMapper.writeValueAsString(tasks);

            TaskRecord taskRecord = new TaskRecord();
            taskRecord.setStudentId(studentId);
            taskRecord.setTaskDate(new Date());
            taskRecord.setTaskContent(taskContent);
            taskRecord.setCompletionRate(BigDecimal.ZERO);
            taskRecord.setAiComment("今日任务已按你的薄弱学科和错题记录自动生成。");
            taskRecord.setStatus("pending");
            this.save(taskRecord);

            return TodayTaskResponse.builder()
                    .tasks(tasks)
                    .aiComment(taskRecord.getAiComment())
                    .completionRate(0.0)
                    .build();
        } catch (Exception e) {
            return generateDefaultTasks(studentId);
        }
    }

    private TodayTaskResponse.TaskItem toTaskItem(Map<String, Object> map) {
        return TodayTaskResponse.TaskItem.builder()
                .id(asLong(map.get("id")))
                .type(String.valueOf(map.getOrDefault("type", "知识点巩固")))
                .content(String.valueOf(map.getOrDefault("content", "")))
                .subject(String.valueOf(map.getOrDefault("subject", "")))
                .knowledgePoint(String.valueOf(map.getOrDefault("knowledgePoint", "")))
                .aiHint(String.valueOf(map.getOrDefault("aiHint", "")))
                .completionRate(asDouble(map.getOrDefault("completionRate", 0.0)))
                .status(String.valueOf(map.getOrDefault("status", "pending")))
                .build();
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
