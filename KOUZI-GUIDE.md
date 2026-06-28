# 扣子 专属开发指南

> AI算法工程师角色 - 负责AI大模型接口对接和智能算法开发

---

## 一、角色定位

**角色**：AI算法工程师

**擅长领域**：NLP、AI模块开发、Prompt工程

**负责模块**：AI大模型接口对接、智能算法

---

## 二、负责任务

| 任务ID | 任务名称 | 优先级 | 依赖任务 |
|--------|---------|-------|---------|
| TASK-AI01 | 高考数据智能抓取接口 | high | TASK-INIT |
| TASK-AI02 | 院校素材智能聚合接口 | high | TASK-AI01 |
| TASK-AI03 | 考点题库智能生成迭代接口 | high | TASK-AI01 |
| TASK-AI04 | 学情分析&任务生成接口 | high | TASK-AI01, TASK-AI03 |
| TASK-AI05 | 段位激励文案生成接口 | medium | TASK-AI01, TASK-AI02 |

---

## 三、技术栈规范

### 3.1 AI技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| LLM API | - | 大模型接口（如GPT、Claude、文心一言等） |
| LangChain | 0.1.x | LLM应用开发框架 |
| SpringBoot | 2.7.18 | 后端框架 |
| Redis | 6.2+ | 缓存LLM输出 |

### 3.2 AI模块目录结构

```
backend/src/main/java/com/example/ai/
├── GaokaoDataService.java          # 高考数据抓取服务
├── CollegeService.java             # 院校聚合服务
├── QuestionBankService.java        # 题库生成服务
├── LearningAnalysisService.java    # 学情分析服务
├── IncentiveService.java           # 激励文案服务
├── crawler/                        # 数据爬取模块
│   └── GaokaoDataCrawler.java
├── prompt/                         # Prompt模板
│   ├── QuestionPrompt.java
│   ├── AnalysisPrompt.java
│   └── IncentivePrompt.java
└── util/                           # AI工具类
    └── LlmApiClient.java
```

---

## 四、开发规范

### 4.1 Prompt工程规范

#### Prompt设计原则

1. **明确任务**：清晰描述要完成的任务
2. **提供上下文**：提供相关背景信息
3. **设定约束**：设定输出格式和限制条件
4. **示例引导**：提供示例输出引导

#### Prompt模板示例

```java
public class QuestionPrompt {
    public static String generateQuestion(String subject, String knowledgePoint, String difficulty) {
        return """
            请生成一道%s学科的%s知识点题目，难度为%s。
            
            要求：
            1. 题目类型：选择题/填空题/解答题
            2. 包含完整的题目描述
            3. 提供正确答案和解析
            4. 难度等级：简单/中等/困难
            
            输出格式：
            {
                "question": "...",
                "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
                "answer": "...",
                "analysis": "...",
                "difficulty": "%s"
            }
            """.formatted(subject, knowledgePoint, difficulty, difficulty);
    }
}
```

### 4.2 AI服务类模板

```java
@Service
public class QuestionBankService {

    @Autowired
    private LlmApiClient llmApiClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public QuestionDTO generateQuestion(QuestionRequest request) {
        String cacheKey = "question:" + request.getSubject() + ":" + request.getKnowledgePoint();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (QuestionDTO) cached;
        }

        String prompt = QuestionPrompt.generateQuestion(
            request.getSubject(),
            request.getKnowledgePoint(),
            request.getDifficulty()
        );

        String response = llmApiClient.call(prompt);
        QuestionDTO question = parseResponse(response);

        redisTemplate.opsForValue().set(cacheKey, question, 24, TimeUnit.HOURS);
        return question;
    }

    private QuestionDTO parseResponse(String response) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response, QuestionDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("解析AI响应失败");
        }
    }
}
```

### 4.3 LLM调用规范

- ✅ 使用Redis缓存高频LLM输出
- ✅ 设置合理的超时时间
- ✅ 处理LLM返回格式异常
- ✅ 记录LLM调用日志
- ✅ 实现降级策略（LLM不可用时使用缓存）

---

## 五、核心算法说明

### 5.1 提分拆分权重算法

| 得分率 | 学科状态 | 时长分配 |
|-------|---------|---------|
| <40% | 极弱学科 | 40% |
| 40%-60% | 薄弱学科 | 25% |
| 60%-80% | 中等学科 | 20% |
| >80% | 优势学科 | 15% |

### 5.2 位次换算算法

```
等效高考分 = 当前分数 × (高考总分 / 模考总分) × 难度系数
等效位次 = 根据等效高考分查询近三年位次表
```

### 5.3 院校匹配算法

```
1. 根据等效位次筛选院校
2. 匹配范围：等效位次±5000名内
3. 同批次院校优先
4. 随机抽取3所展示
```

---

## 六、验收标准

### 6.1 功能验收

- [ ] AI接口调用成功
- [ ] 输出格式符合要求
- [ ] 算法逻辑正确
- [ ] 缓存机制生效

### 6.2 技术验收

- [ ] Prompt设计合理
- [ ] 错误处理完善
- [ ] 日志记录完整
- [ ] 降级策略有效

---

## 七、参考文档

| 文档 | 用途 |
|-----|------|
| [AI快捷执行手册.md](AI快捷执行手册.md) | 任务卡片和执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构和命名规范 |
| [API接口契约规范.md](API接口契约规范.md) | 接口定义和Mock数据策略 |