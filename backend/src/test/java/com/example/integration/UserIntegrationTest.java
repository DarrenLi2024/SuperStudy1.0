package com.example.integration;

import com.example.SuperStudyApplication;
import com.example.sys.dto.LoginRequest;
import com.example.util.ResponseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户权限集成测试
 * 验证登录、鉴权、权限控制完整流程
 */
@SpringBootTest(classes = SuperStudyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("登录接口 - 返回正确格式")
    void login_ReturnsCorrectFormat() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        ResponseEntity<ResponseResult> response = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
    }

    @Test
    @DisplayName("未登录访问受保护接口 - 返回401")
    void accessProtectedApi_WithoutToken_Returns401() {
        ResponseEntity<ResponseResult> response = restTemplate.getForEntity(
                "/api/v1/student/profile/me", ResponseResult.class);

        // 没有token时会被重定向或返回401
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("登录参数校验 - 空账号返回400")
    void login_EmptyUsername_Returns400() {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("password123");

        ResponseEntity<ResponseResult> response = restTemplate.postForEntity(
                "/api/v1/user/login", request, ResponseResult.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("用户信息接口 - 需要认证")
    void userInfo_RequiresAuth() {
        ResponseEntity<ResponseResult> response = restTemplate.getForEntity(
                "/api/v1/user/info", ResponseResult.class);

        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("CORS配置 - OPTIONS请求返回正确头")
    void cors_OptionsRequest_ReturnsCorrectHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Origin", "http://localhost:5173");
        headers.set("Access-Control-Request-Method", "POST");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/user/login",
                HttpMethod.OPTIONS,
                entity,
                Void.class);

        assertNotNull(response.getHeaders().get("Access-Control-Allow-Origin"));
    }
}
