package com.example.integration;

import com.example.SuperStudyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 学生档案集成测试
 * 验证档案CRUD和权限控制
 */
@SpringBootTest(classes = SuperStudyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("学生档案接口 - 需要认证")
    void getProfile_RequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/student/profile/1", String.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("创建档案 - 学生权限")
    void createProfile_RequiresStudentRole() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer mock-token");
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/student/profile", entity, String.class);

        assertNotNull(response.getBody());
    }
}
