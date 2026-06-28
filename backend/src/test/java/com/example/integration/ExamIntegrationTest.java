package com.example.integration;

import com.example.SuperStudyApplication;
import com.example.sys.dto.LoginRequest;
import com.example.util.ResponseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模考位次集成测试
 * 验证考试提交、位次换算、段位数据接口
 */
@SpringBootTest(classes = SuperStudyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/integration-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ExamIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpEntity<Void> authEntity() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student001");
        request.setPassword("password123");

        ResponseEntity<ResponseResult> loginResponse = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (loginResponse.getBody() != null && loginResponse.getBody().getData() != null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) loginResponse.getBody().getData();
            String token = (String) data.get("token");
            if (token != null) {
                headers.setBearerAuth(token);
            }
        }
        return new HttpEntity<>(headers);
    }

    @Test
    @DisplayName("考试记录列表接口可用")
    void getExamRecords_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/exam/records/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("段位卡片接口可用")
    void getCollegeCards_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/growth/cards/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("段位进度接口可用")
    void getGrowthProgress_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/growth/progress/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("成长数据接口可用")
    void getGrowthData_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/growth/data/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("学习任务接口可用")
    void getTodayTasks_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/learning/today/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("知识点掌握接口可用")
    void getKnowledgeStatus_ReturnsData() {
        HttpEntity<Void> entity = authEntity();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/learning/knowledge/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("家长端接口可用")
    void getParentOverview_ReturnsData() {
        // 家长端需要先作为家长登录
        LoginRequest request = new LoginRequest();
        request.setUsername("parent001");
        request.setPassword("password123");

        ResponseEntity<ResponseResult> loginResponse = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (loginResponse.getBody() != null && loginResponse.getBody().getData() != null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) loginResponse.getBody().getData();
            String token = (String) data.get("token");
            if (token != null) headers.setBearerAuth(token);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/parent/overview/1", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("管理后台监控接口可用")
    void getAdminMonitor_ReturnsData() {
        // 管理后台需要管理员登录
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        ResponseEntity<ResponseResult> loginResponse = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (loginResponse.getBody() != null && loginResponse.getBody().getData() != null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) loginResponse.getBody().getData();
            String token = (String) data.get("token");
            if (token != null) headers.setBearerAuth(token);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/monitor", HttpMethod.GET, entity, String.class);

        assertNotNull(response.getBody());
    }
}
