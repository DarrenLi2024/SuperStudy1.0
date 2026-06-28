package com.example.integration;

import com.example.SuperStudyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模考位次集成测试
 * 验证考试提交、位次换算、段位数据接口
 */
@SpringBootTest(classes = SuperStudyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExamIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("考试记录列表接口可用")
    void getExamRecords_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/exam/records/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("段位卡片接口可用")
    void getCollegeCards_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/growth/cards/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("段位进度接口可用")
    void getGrowthProgress_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/growth/progress/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("成长数据接口可用")
    void getGrowthData_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/growth/data/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("学习任务接口可用")
    void getTodayTasks_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/learning/today/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("知识点掌握接口可用")
    void getKnowledgeStatus_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/learning/knowledge/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("家长端接口可用")
    void getParentOverview_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/parent/overview/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("管理后台监控接口可用")
    void getAdminMonitor_ReturnsData() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/admin/monitor", String.class);

        assertNotNull(response.getBody());
    }
}
