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
 * 学生档案集成测试
 * 验证档案CRUD和权限控制
 */
@SpringBootTest(classes = SuperStudyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/integration-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class StudentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("未认证访问学生档案 - 返回401")
    void getProfile_RequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/student/profile/1", String.class);

        assertTrue(response.getStatusCodeValue() == 401 || response.getStatusCodeValue() == 403);
    }

    @Test
    @DisplayName("使用学生Token访问档案 - 返回成功")
    void getProfile_WithStudentToken() {
        // 学生登录
        LoginRequest request = new LoginRequest();
        request.setUsername("student001");
        request.setPassword("password123");

        ResponseEntity<ResponseResult> loginResponse = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        assertEquals(200, loginResponse.getStatusCodeValue());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (loginResponse.getBody() != null && loginResponse.getBody().getData() != null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) loginResponse.getBody().getData();
            String token = (String) data.get("token");
            if (token != null) headers.setBearerAuth(token);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/student/profile/1", HttpMethod.GET, entity, String.class);

        assertTrue(response.getStatusCodeValue() == 200 || response.getStatusCodeValue() == 302);
    }

    @Test
    @DisplayName("创建档案 - 无效Token返回401")
    void createProfile_RequiresStudentRole() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer mock-token");
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/student/profile", entity, String.class);

        assertTrue(response.getStatusCodeValue() == 401 || response.getStatusCodeValue() == 403);
    }
}
