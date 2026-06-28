# 审计差距整改开发计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将当前“可展示，不可生产”的 MVP 提升为具备真实 AI 闭环、定时自治、可验证质量门禁的交付版本。

**Architecture:** 保留现有 Vue3 + SpringBoot + MySQL + Redis 架构，不重写工程骨架。优先补齐 AI 大模型中枢层、定时自治任务和位次院校匹配逻辑，再处理前端体验和测试覆盖。

**Tech Stack:** SpringBoot 2.7.18、MyBatis-Plus 3.5.3、SpringSecurity + JWT、MySQL 8.0、Redis 6.2、Vue3 + Vite4、Element Plus、JUnit 5、Mockito、SpringBoot Test。

---

## 一、计划依据

**已阅读文档：**
- `deploy/delivery-audit.md`
- `AI快捷执行手册.md`
- `迭代任务映射表.md`
- `系统开发计划.md`
- `API接口契约规范.md`
- `项目目录结构规范.md`
- `多AI协作开发总纲.md`
- `CODEX-GUIDE.md`
- `CLAUDE-GUIDE.md`

**审计结论：**
- 综合交付就绪度：55%。
- API 端点覆盖率：100%，主要问题不是接口缺失，而是核心实现空壳。
- P0 阻塞：AI 大模型中枢层约 5%，定时自治任务约 20%。
- P1 风险：位次换算固定系数、院校匹配不按位次区间、`score_rank` 数据年份和省份过窄。
- P2 体验缺口：题目硬编码、月报占位、热力图缺失、段位升级动效缺失。

**项目文档完整性检查：**
- 根目录已有 `AGENTS.md`、`README.md`、`CONTRIBUTING.md`、`CHANGELOG.md`、各 AI 专属 Guide、接口契约、目录规范、系统计划、任务映射、UI 规范、审计报告。
- 缺失计划目录已补齐：`docs/plans/`。

---

## 二、执行原则

1. 先补 P0，再补 P1，最后做 P2/P3。
2. AI 输出必须可降级：LLM 失败时返回结构化错误或缓存结果，不返回伪 AI 文案冒充成功。
3. 所有外部数据自动进入自治表，不新增任何人工上传、录入、编辑素材入口。
4. 先写失败测试，再实现最小代码，再跑测试。
5. 每个任务完成后更新 `AI快捷执行手册.md` 中对应任务状态和执行日志。

---

## 三、里程碑安排

| 阶段 | 周期 | 目标 | 退出标准 |
|------|------|------|----------|
| M1 P0-AI中枢 | 5-7天 | 接入 LLM Client，题库、学情、激励文案从空壳变为可调用服务 | AI 服务可配置、可重试、可缓存、测试通过 |
| M2 P0-定时自治 | 2-3天 | 定时同步数据、院校素材、题库迭代、备份脚本接入 | Redis 有任务日志，MySQL 备份可验证 |
| M3 P1-位次与院校精度 | 2-4天 | 动态折算、近三年位次、院校按位次区间匹配 | 不再依赖固定 0.85 和 batch 等值随机 |
| M4 P2-前端体验 | 3-5天 | 替换硬编码题、热力图、月报、段位升级动效 | 页面无占位字符串，核心流程可演示 |
| M5 质量门禁 | 3-5天 | 单元测试、集成测试、性能与红线审计 | 核心业务测试覆盖，红线 10/10 通过 |

---

## 四、任务拆解

### Task 1: 建立统一 LLM 调度层

**负责人：** 扣子 + Claude Code  

**Files:**
- Create: `backend/src/main/java/com/example/ai/client/LlmClient.java`
- Create: `backend/src/main/java/com/example/ai/client/LlmRequest.java`
- Create: `backend/src/main/java/com/example/ai/client/LlmResponse.java`
- Create: `backend/src/main/java/com/example/ai/client/DefaultLlmClient.java`
- Create: `backend/src/main/java/com/example/ai/config/LlmProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-test.yml`
- Test: `backend/src/test/java/com/example/ai/client/DefaultLlmClientTest.java`

**Steps:**
1. 写 `DefaultLlmClientTest`，覆盖 API Key 缺失、正常响应、超时重试、JSON 解析失败四种场景。
2. 新增 `LlmProperties`，配置 `provider`、`baseUrl`、`apiKey`、`model`、`timeoutMs`、`maxRetries`。
3. 实现 `LlmClient`，所有 AI 服务只依赖接口，不直接写 HTTP 细节。
4. 增加 Redis 缓存策略：相同 prompt hash 可短期复用，避免重复生成。
5. LLM 不可用时返回业务异常 `503`，禁止静默返回硬编码伪结果。

**Verify:**
- Run: `cd backend && mvn test -Dtest=DefaultLlmClientTest`
- Expected: PASS。

---

### Task 2: 实现智能题库生成闭环

**负责人：** 扣子 + Claude Code  

**Files:**
- Modify: `backend/src/main/java/com/example/ai/QuestionBankService.java`
- Create: `backend/src/main/java/com/example/ai/QuestionBankServiceImpl.java`
- Modify: `backend/src/main/java/com/example/ai/generator/QuestionGenerator.java`
- Modify: `backend/src/main/java/com/example/question/QuestionController.java`
- Modify: `backend/src/main/resources/sql/init.sql`
- Test: `backend/src/test/java/com/example/ai/QuestionBankServiceTest.java`
- Test: `backend/src/test/java/com/example/question/QuestionControllerTest.java`

**Steps:**
1. 写测试：给定学生 450 分、数学薄弱，生成题目应包含基础题占比最高、知识点标签、难度标签、答案。
2. 为 `ai_question_bank` 补字段：`options_json`、`analysis`、`quality_score`、`usage_count`、`source_type`。
3. 实现 `QuestionGenerator` 的 prompt 模板，输出强制 JSON Schema。
4. `QuestionBankServiceImpl.generateTrainingQuestions()` 先查题库，不足时调用 LLM 生成并入库。
5. `QuestionController` 删除本地样例题模板，改为调用 `QuestionBankService`。
6. 实现 `eliminateLowQualityQuestions()` 和 `replenishQuestionBank()` 供定时任务调用。

**Verify:**
- Run: `cd backend && mvn test -Dtest=QuestionBankServiceTest,QuestionControllerTest`
- Expected: PASS。

---

### Task 3: 实现学情分析和任务生成

**负责人：** 扣子 + Claude Code  

**Files:**
- Modify: `backend/src/main/java/com/example/ai/LearningAnalysisService.java`
- Create: `backend/src/main/java/com/example/ai/LearningAnalysisServiceImpl.java`
- Modify: `backend/src/main/java/com/example/learning/LearningServiceImpl.java`
- Modify: `backend/src/main/java/com/example/learning/entity/TaskRecord.java`
- Test: `backend/src/test/java/com/example/ai/LearningAnalysisServiceTest.java`
- Test: `backend/src/test/java/com/example/learning/LearningServiceTest.java`

**Steps:**
1. 写测试：高二文科 450 分、数学 50 分极弱时，数学任务时长权重应约 40%。
2. `LearningAnalysisServiceImpl` 汇总考试记录、错题、知识点掌握度，生成结构化每日任务。
3. `LearningServiceImpl.getTodayTasks()` 改为当天无任务时调用 AI 服务并持久化，不再随机模板。
4. `recordErrorQuestion()` 接入 `analyzeErrorCause()`，保存 `ai_analysis`。
5. `generateWeeklyReport()` 和 `generateMonthlyReport()` 返回真实报告或明确 AI 不可用错误，不再返回占位字符串。

**Verify:**
- Run: `cd backend && mvn test -Dtest=LearningAnalysisServiceTest,LearningServiceTest`
- Expected: PASS。

---

### Task 4: 实现段位激励文案和升级记录

**负责人：** 扣子 + Claude Code  

**Files:**
- Modify: `backend/src/main/java/com/example/ai/IncentiveService.java`
- Create: `backend/src/main/java/com/example/ai/IncentiveServiceImpl.java`
- Modify: `backend/src/main/java/com/example/exam/ExamServiceImpl.java`
- Modify: `backend/src/main/java/com/example/growth/GrowthServiceImpl.java`
- Test: `backend/src/test/java/com/example/ai/IncentiveServiceTest.java`
- Test: `backend/src/test/java/com/example/growth/GrowthServiceTest.java`

**Steps:**
1. 写测试：成绩跨批次时必须插入 `growth_record`。
2. `ExamServiceImpl.submitExam()` 计算新旧批次，发生升级时调用 `IncentiveService.generateUpgradeIncentive()`。
3. `generateIncentive()` 私有 if-else 文案改为 `IncentiveService` 调用。
4. 激励文案限制长度、语气和敏感词，避免过度承诺。
5. `GrowthServiceImpl.getGrowthHistory()` 确认返回升级记录和 AI 文案。

**Verify:**
- Run: `cd backend && mvn test -Dtest=IncentiveServiceTest,GrowthServiceTest`
- Expected: PASS。

---

### Task 5: 打通定时自治任务

**负责人：** WorkBuddy + Claude Code  

**Files:**
- Modify: `backend/src/main/java/com/example/task/ScheduledTasks.java`
- Modify: `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- Create: `backend/src/main/java/com/example/ai/GaokaoDataServiceImpl.java`
- Modify: `backend/src/main/java/com/example/ai/CollegeService.java`
- Create: `backend/src/main/java/com/example/ai/CollegeServiceImpl.java`
- Modify: `scripts/backup.sh`
- Modify: `scripts/health_check.sh`
- Test: `backend/src/test/java/com/example/task/ScheduledTasksTest.java`

**Steps:**
1. 写测试：手动调用 `syncScoreRankData()` 后应调用 `GaokaoDataService` 并写 Redis 日志。
2. 实现高考一分一段数据同步：支持省份、年份、科类去重写入。
3. 实现院校素材刷新：官方公开源优先，LOGO 缓存到 `backend/src/main/resources/static/logos/` 或配置路径。
4. `refreshCollegeAndQuestions()` 接入 `CollegeService`、`QuestionBankService`。
5. `recordHeartbeat()` 改名或拆分，真正触发 `scripts/backup.sh` 并记录备份状态。
6. 失败时写 Redis 任务日志，保留最近 50 条。

**Verify:**
- Run: `cd backend && mvn test -Dtest=ScheduledTasksTest`
- Run: `bash scripts/health_check.sh`
- Expected: PASS，且备份脚本可产生可恢复文件。

---

### Task 6: 修复位次换算与院校匹配

**负责人：** Claude Code + Codex  

**Files:**
- Modify: `backend/src/main/java/com/example/exam/ExamServiceImpl.java`
- Modify: `backend/src/main/java/com/example/mapper/ScoreRankMapper.java`
- Modify: `backend/src/main/java/com/example/mapper/CollegeBasicMapper.java`
- Modify: `backend/src/main/resources/mapper/CollegeBasicMapper.xml`
- Modify: `backend/src/main/resources/sql/init.sql`
- Test: `backend/src/test/java/com/example/exam/ExamServiceTest.java`

**Steps:**
1. 写测试：同一学生不同考试类型应使用动态折算，不固定乘 0.85。
2. `findRankByScore()` 按学生省份、科类、近三年数据查询，优先精确分数，缺失时线性插值。
3. 院校匹配从 `admission_batch = batch` 改为位次窗口匹配：当前位次 ±5000。
4. `college_basic` 补充参考录取位次字段，例如 `min_rank`、`max_rank`、`province`、`year`。
5. 保持“仅稳妥院校对标”：不得展示冲刺、保底标签。

**Verify:**
- Run: `cd backend && mvn test -Dtest=ExamServiceTest`
- Expected: PASS。

---

### Task 7: 前端替换占位体验

**负责人：** Cursor  

**Files:**
- Modify: `frontend/src/views/student/exam/index.vue`
- Modify: `frontend/src/views/student/learning/index.vue`
- Modify: `frontend/src/views/student/growth/index.vue`
- Modify: `frontend/src/views/student/home/index.vue`
- Modify: `frontend/src/components/GrowthProgress.vue`
- Create: `frontend/src/components/KnowledgeHeatmap.vue`
- Create: `frontend/src/components/UpgradeDialog.vue`
- Modify: `frontend/src/api/question.js`
- Modify: `frontend/src/api/learning.js`
- Modify: `frontend/src/api/growth.js`

**Steps:**
1. 模考中心题目来源改为 `/api/v1/question/training/{studentId}`。
2. 学习中心增加每日打卡状态，知识点掌握度用热力图展示。
3. 成长数据页月报接入后端 AI 月报字段，不展示占位字符串。
4. 学生首页监听成长记录新增，展示段位升级弹窗和进度动效。
5. 管理后台接入 Redis 中的定时任务日志和备份状态。

**Verify:**
- Run: `cd frontend && npm run build`
- Expected: build success。

---

### Task 8: 安全、权限和质量门禁

**负责人：** Codex + Trae  

**Files:**
- Modify: `backend/src/main/java/com/example/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/example/util/TokenRedisService.java`
- Modify: `backend/src/test/java/com/example/integration/UserIntegrationTest.java`
- Modify: `backend/src/test/java/com/example/integration/StudentIntegrationTest.java`
- Modify: `backend/src/test/java/com/example/integration/ExamIntegrationTest.java`
- Create: `backend/src/test/java/com/example/integration/AiWorkflowIntegrationTest.java`
- Create: `backend/src/test/java/com/example/integration/PermissionIsolationIntegrationTest.java`

**Steps:**
1. 写集成测试：被 Redis 踢下线的旧 Token 必须返回 401。
2. 写权限测试：家长只能读绑定学生数据，管理员不能访问学习答题接口。
3. 写 AI 工作流测试：提交考试、生成任务、生成补强题、写成长记录能串联通过。
4. 增加 JaCoCo 或 Maven 覆盖率检查，核心 Service 覆盖率目标 ≥80%。
5. 运行全量后端测试和前端构建。

**Verify:**
- Run: `cd backend && mvn test`
- Run: `cd frontend && npm run build`
- Expected: 全部通过。

---

## 五、验收清单

**2026-06-29 执行复核：**
- [x] 已建立统一 LLM 调度层，生产 profile 缺少真实 AI 配置时返回明确 503。
- [x] 已清理前端 Mock 登录、Mock 成功、Mock 数据兜底和固定 1 号学生访问。
- [x] 已补齐学生/家长资源级读取校验，提交类接口强制使用当前绑定学生。
- [x] 已打通模考取题、提交成绩、刷新记录的前后端闭环。
- [x] 已接入管理端真实运行指标、Redis 状态、备份状态和 AI 配置状态。
- [x] 已补知识点热力图组件，并移除前端硬编码题库。
- [!] 当前机器缺少 Java Runtime 与 Maven，后端编译和测试仍未验证。
- [!] 高考一分一段、院校录取位次和院校素材仍缺真实官方数据源接入；现有同步服务只能作为自动化框架和兜底估算，不能作为最终生产数据质量依据。

**P0 必过：**
- [ ] AI 题库生成不再硬编码。
- [ ] AI 每日任务、周报、月报不再返回占位字符串。
- [ ] 激励文案由 AI 服务生成，并有失败降级策略。
- [ ] 定时任务不再是 TODO，能同步数据、刷新院校、迭代题库、触发备份。
- [ ] 所有定时任务有 Redis 日志和失败记录。

**P1 必过：**
- [ ] 位次换算不再固定 0.85。
- [ ] 院校匹配按位次区间，不按批次随机。
- [ ] `score_rank` 支持近三年、至少物理/历史两类数据。
- [ ] 段位升级记录入库。
- [ ] Token 二次校验覆盖所有需要鉴权的请求。

**P2 必过：**
- [ ] 前端无固定 12 道硬编码题。
- [ ] 成长月报无占位文案。
- [ ] 知识点热力图上线。
- [ ] 段位升级动效上线。
- [ ] 管理后台展示运行日志和备份状态。

**红线复核：**
- [ ] 无上传/录入/编辑素材入口。
- [ ] 无题库管理后台。
- [ ] 无院校素材上传。
- [ ] 无高考数据录入。
- [ ] 家长端无编辑按钮。
- [ ] 三角色权限强隔离。
- [ ] 仅稳妥院校对标。
- [ ] AI 每日任务千人千面。
- [ ] 段位升级动效和激励文案完整。
- [ ] 零人工运维闭环成立。

---

## 六、推荐执行顺序

1. `Task 1` 先完成，所有 AI 功能依赖统一 LLM 调度层。
2. `Task 2`、`Task 3`、`Task 4` 可并行，但必须复用 `LlmClient`。
3. `Task 5` 在 `Task 2` 至少完成题库接口后开始。
4. `Task 6` 可与 `Task 2-4` 并行，但合并时由 Codex 做回归测试。
5. `Task 7` 等后端接口稳定后开始。
6. `Task 8` 全程穿插，最终作为交付门禁。

---

## 七、交接要求

每完成一个任务，必须更新：
- `AI快捷执行手册.md`：任务状态和执行日志。
- `CHANGELOG.md`：用户可见变化。
- 对应测试文件：失败用例、修复实现、通过记录。

每次交接必须提供：
- 修改文件列表。
- 已运行命令和结果。
- 未解决风险。
- 是否触碰开发红线。
