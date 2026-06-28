# Codex 专属开发指南

> 测试工程师角色 - 负责代码质量保障和测试编写

---

## 一、角色定位

**角色**：测试工程师

**擅长领域**：代码补全、单元测试、代码审查

**负责模块**：单元测试、代码优化、Bug修复

---

## 二、负责任务

| 任务ID | 任务名称 | 优先级 | 依赖任务 |
|--------|---------|-------|---------|
| TASK-TEST01 | 单元测试编写 | medium | 对应模块完成后 |
| TASK-TEST02 | 集成测试编写 | medium | 单元测试通过后 |
| TASK-TEST03 | 性能优化与Bug修复 | medium | 测试执行后 |

---

## 三、技术栈规范

### 3.1 测试技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| JUnit | 5.x | 单元测试框架 |
| Mockito | 4.x | Mock框架 |
| SpringBoot Test | 2.7.x | 集成测试 |
| JMeter | 5.x | 性能测试 |

### 3.2 测试目录结构

```
backend/src/test/java/com/example/
├── sys/
│   ├── UserControllerTest.java
│   └── UserServiceTest.java
├── student/
│   └── StudentServiceTest.java
├── exam/
│   └── ExamServiceTest.java
├── learning/
│   └── LearningServiceTest.java
└── growth/
    └── GrowthServiceTest.java
```

---

## 四、测试规范

### 4.1 单元测试规范

#### 测试类命名

| 类型 | 规范 | 示例 |
|-----|------|-----|
| 测试类 | `{被测类}Test` | `UserServiceTest` |
| 测试方法 | `{场景}_{预期结果}` | `testLoginSuccess` |

#### 测试模板

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private SysUserMapper userMapper;

    @Test
    void testLoginSuccess() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("123456");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPassword(BCryptPasswordEncoder.encode("123456"));
        user.setRole("STUDENT");

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginVO result = userService.login(dto);

        assertNotNull(result);
        assertEquals("STUDENT", result.getRole());
    }

    @Test
    void testLoginFail() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("wrong");

        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.login(dto));
    }
}
```

### 4.2 集成测试规范

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testLoginApi() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800138000");
        dto.setPassword("123456");

        ResponseEntity<ResponseResult> response = restTemplate.postForEntity(
            "/api/v1/sys/login", dto, ResponseResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
    }
}
```

### 4.3 测试覆盖率要求

| 指标 | 要求 |
|-----|------|
| 单元测试覆盖率 | ≥80% |
| 核心业务覆盖 | 100% |
| 集成测试覆盖 | 核心流程100% |

---

## 五、代码审查规范

### 5.1 审查内容

| 检查项 | 检查内容 |
|-------|---------|
| 代码质量 | 方法职责、代码复用、复杂度 |
| 安全合规 | 密码加密、SQL注入防护、输入校验 |
| 命名规范 | 类名、方法名、变量名符合规范 |
| 错误处理 | 异常处理、日志记录 |
| 性能问题 | N+1查询、未使用缓存 |

### 5.2 审查流程

1. 阅读代码，理解业务逻辑
2. 运行测试，检查测试覆盖率
3. 审查代码质量和安全合规
4. 输出审查报告，列出问题和改进建议

---

## 六、Bug修复规范

### 6.1 Bug记录格式

```
【Bug ID】BUG-001
【模块】用户权限服务
【描述】登录接口密码错误时返回500错误
【复现步骤】
1. 访问登录接口
2. 输入正确手机号和错误密码
3. 接口返回500错误
【预期结果】返回400错误，提示密码错误
【修复方案】在Service层捕获密码不匹配异常，返回正确错误码
```

### 6.2 修复流程

1. 确认Bug描述和复现步骤
2. 定位问题代码
3. 编写测试用例验证Bug
4. 修复代码
5. 运行测试确认修复成功
6. 更新Bug状态为已修复

---

## 七、性能优化规范

### 7.1 性能指标

| 指标 | 要求 |
|-----|------|
| API响应时间 | <200ms |
| 数据库查询时间 | <100ms |
| 并发用户数 | ≥100 |

### 7.2 优化策略

- ✅ 数据库索引优化
- ✅ Redis缓存优化
- ✅ 代码逻辑优化
- ✅ 异步处理优化

---

## 八、验收标准

### 8.1 测试验收

- [ ] 单元测试覆盖率≥80%
- [ ] 所有测试用例通过
- [ ] 集成测试覆盖核心流程

### 8.2 代码审查验收

- [ ] 代码质量符合规范
- [ ] 安全合规检查通过
- [ ] 性能问题已修复

---

## 九、参考文档

| 文档 | 用途 |
|-----|------|
| [AI快捷执行手册.md](AI快捷执行手册.md) | 任务卡片和执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构和命名规范 |
| [API接口契约规范.md](API接口契约规范.md) | 接口定义和Mock数据策略 |