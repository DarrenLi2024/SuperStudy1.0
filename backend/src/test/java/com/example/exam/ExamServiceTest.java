package com.example.exam;

import com.example.entity.CollegeBasic;
import com.example.exam.entity.ExamRecord;
import com.example.exam.mapper.ExamRecordMapper;
import com.example.mapper.CollegeBasicMapper;
import com.example.mapper.ScoreRankMapper;
import com.example.student.entity.StudentProfile;
import com.example.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRecordMapper examRecordMapper;

    @Mock
    private ScoreRankMapper scoreRankMapper;

    @Mock
    private CollegeBasicMapper collegeBasicMapper;

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @InjectMocks
    private ExamServiceImpl examService;

    @BeforeEach
    void setUp() {
        injectField(examService, "baseMapper", examRecordMapper);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("批次计算测试")
    class BatchCalculationTests {

        @Test
        @DisplayName("211批次判定")
        void calculateBatch_211() {
            assertEquals("211/双一流", examService.calculateBatch(600));
            assertEquals("211/双一流", examService.calculateBatch(550));
        }

        @Test
        @DisplayName("一本批次判定")
        void calculateBatch_FirstClass() {
            assertEquals("普通一本", examService.calculateBatch(500));
            assertEquals("普通一本", examService.calculateBatch(480));
        }

        @Test
        @DisplayName("二本批次判定")
        void calculateBatch_SecondClass() {
            assertEquals("公办二本", examService.calculateBatch(450));
            assertEquals("公办二本", examService.calculateBatch(400));
        }

        @Test
        @DisplayName("本科以下")
        void calculateBatch_Below() {
            assertEquals("本科以下", examService.calculateBatch(350));
            assertEquals("本科以下", examService.calculateBatch(0));
            assertEquals("未知", examService.calculateBatch(null));
        }
    }

    @Nested
    @DisplayName("考试提交测试")
    class ExamSubmitTests {

        @Test
        @DisplayName("提交考试成功")
        void submitExam_Success() {
            when(examRecordMapper.insert(any())).thenReturn(1);

            com.example.dto.request.ExamSubmitRequest request = new com.example.dto.request.ExamSubmitRequest();
            request.setExamType("weekly");
            request.setExamDate("2026-06-28");
            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("语文", 90);
            scores.put("数学", 75);
            scores.put("英语", 85);
            scores.put("历史", 65);
            scores.put("政治", 60);
            scores.put("地理", 70);
            request.setSubjectScores(scores);

            ExamRecord record = examService.submitExam(1L, request);

            assertNotNull(record);
            assertEquals(1L, record.getStudentId());
            assertEquals(445, record.getTotalScore());
            assertNotNull(record.getEquivalentGaokaoScore());
            assertNotNull(record.getCurrentBatch());
            assertEquals(378, record.getEquivalentGaokaoScore()); // 445 * 0.85 = 378
        }
    }

    @Nested
    @DisplayName("段位进度测试")
    class GrowthProgressTests {

        @Test
        @DisplayName("获取进度条数据")
        void getGrowthProgress_Success() {
            StudentProfile profile = new StudentProfile();
            profile.setId(1L);
            profile.setBaselineScore(450);
            profile.setTargetScore(600);
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            var progress = examService.getGrowthProgress(1L);

            assertNotNull(progress);
            assertEquals(3, progress.getPhases().size());
            assertEquals(600, progress.getTargetScore());
            assertEquals("二本阶段", progress.getPhases().get(0).getName());
            assertEquals("一本阶段", progress.getPhases().get(1).getName());
            assertEquals("211/双一流阶段", progress.getPhases().get(2).getName());
        }

        @Test
        @DisplayName("分数在二本阶段内的进度计算")
        void getGrowthProgress_SecondClassPhase() {
            StudentProfile profile = new StudentProfile();
            profile.setId(1L);
            profile.setBaselineScore(430);
            profile.setTargetScore(600);
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            var progress = examService.getGrowthProgress(1L);

            // 430 / (480-400) = 37.5%
            assertTrue(progress.getPhases().get(0).getProgress() > 0);
            assertFalse(progress.getPhases().get(0).getCompleted());
        }
    }

    @Nested
    @DisplayName("院校卡片测试")
    class CollegeCardTests {

        @Test
        @DisplayName("获取院校卡片数据")
        void getCollegeCards_Success() {
            StudentProfile profile = new StudentProfile();
            profile.setId(1L);
            profile.setBaselineScore(450);
            profile.setTargetScore(600);
            profile.setDreamCollege("北京大学");
            profile.setDreamCollegeBatch("双一流");
            when(studentProfileMapper.selectById(1L)).thenReturn(profile);

            // Mock随机院校
            CollegeBasic mockCollege = new CollegeBasic();
            mockCollege.setId(1L);
            mockCollege.setCollegeName("测试大学");
            mockCollege.setAdmissionBatch("公办二本");
            mockCollege.setLogoPath("/logos/test.png");
            when(collegeBasicMapper.selectRandomByBatch(anyString()))
                    .thenReturn(Collections.singletonList(mockCollege));

            var cards = examService.getCollegeCards(1L);

            assertNotNull(cards);
            assertNotNull(cards.getCurrentBatchCards());
            assertNotNull(cards.getDreamCollege());
            assertEquals(450, cards.getCurrentScore());
            assertEquals("公办二本", cards.getCurrentBatch());
        }
    }
}
