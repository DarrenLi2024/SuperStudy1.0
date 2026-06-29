# 变更日志

> 记录项目的所有重要变更

---

## 格式说明

```
## [版本号] - 日期

### 新增
- 功能描述

### 修复
- Bug描述

### 变更
- 变更描述

### 移除
- 移除描述

### 安全
- 安全修复描述
```

---

## [1.1.0] - 2026-06-30

### AI Native 全线打通 — 核心修复

**新增**
- **AiController** — 统一 AI 接入端点 (`/api/v1/ai/generate` 同步 + `/api/v1/ai/stream` SSE流式)
- **AiController.status** — AI 服务健康检查端点 (`GET /api/v1/ai/status`)
- **前端 api/ai.js** — AI 服务 API 层 (`aiGenerate`, `getAiStatus`)
- **CollegeDataProvider / GaokaoDataProvider** — 重命名消除"爬虫"命名欺诈，明确标注 LLM 驱动
- LearningServiceImpl AI 点评 — 今日任务生成时通过 LLM 生成真实 AI 点评

**修复**
- **降级题目答案不再固定为 A** — `QuestionGeneratorImpl.fallbackQuestion` 和 `DefaultLlmClient.generateFallbackQuestions` 基于 seed 确定性变化
- **ExamServiceImpl 诊断报告接入真实 LLM** — `generateDiagnosis` 从模板字符串改为 LLM 优先 + 规则降级
- **GrowthServiceImpl 注入 AI 服务** — 段位升级和成长数据通过 IncentiveService/LearningAnalysisService 生成 AI 内容
- **useAI.js fallback 路径修复** — SSE 失败时正确降级到 `/api/v1/ai/generate`
- **application-prod.yml AI 配置完善** — 明确标注配置说明，未设置时安全降级
- **application-dev.yml** — 增加三种启用 AI 的详细说明（环境变量/命令行/Ollama）

**移除**
- CollegeCrawler / CollegeCrawlerImpl — 被 CollegeDataProvider/Impl 替代
- GaokaoDataCrawler / GaokaoDataCrawlerImpl — 被 GaokaoDataProvider/Impl 替代

**变更**
- LearningAnalysisServiceImpl.latestSubjectScores — `.last("LIMIT 1")` 改为 MyBatis-Plus Page 对象（消除 SQL 注入风险）

---
## [1.0.0] - 2026-06-28

### 新增
- 项目初始化，创建SpringBoot + Vue3项目骨架
- 用户权限服务（登录、权限验证、SecurityUtils、TokenRedisService）
- 学生档案服务（基础信息管理、权限隔离）
- 模考位次换算服务（分数↔位次双向映射、院校匹配、段位进度计算）
- AI大模型中枢层（高考数据抓取、院校聚合、题库生成、学情分析、激励文案）
- 段位激励服务（段位计算、院校卡片、成长数据、升级记录）
- AI自适应学习服务（任务生成、错题记录、知识点热力图）
- 定时自治任务（Spring Scheduled每日/每周任务）
- 管理员后台（用户管理、系统监控、AI参数配置）
- 前端页面（登录页、学生首页、学习中心、模考中心、成长数据页、家长端、管理后台）
- 前端API文件（user/student/exam/learning/growth/parent/admin/question）
- Mock数据策略和API接口契约定义
- 多AI协作开发文档和任务卡片

### 修复
- 暂无

### 变更
- 暂无

### 移除
- 暂无

### 安全
- 密码使用BCrypt加密存储
- JWT令牌认证
- SpringSecurity权限控制
- 输入参数校验

---

## [1.1.0] - 待发布

### 新增
- 统一 LLM 调度层，支持真实模型配置、本地确定性降级和 Redis 缓存
- 智能题库生成闭环，专项训练/补强训练接口改为调用 `QuestionBankService`
- 学情分析服务，支持薄弱学科识别、学习时长权重、每日任务、周/月报和错因分析
- 段位激励文案服务，支持升级、提分、停滞和每日点评场景
- 一分一段同步和院校素材刷新基础服务
- 知识点热力图前端组件
- 资源级学生访问控制服务，统一校验学生本人、绑定家长和管理员访问边界
- 后端关键单元测试：LLM Client、题库生成、学情权重
- 集成测试覆盖
- Docker容器化部署
- 自动化备份脚本
- 监控告警配置
- 性能优化

### 修复
- 修复前端登录失败后可生成本地 Token 绕过真实鉴权的问题
- 修复登录后未保存绑定 `studentId` 导致页面默认访问 1 号学生的问题
- 修复学生、家长、成长、学习、模考接口缺少资源级读取校验的问题
- 修复管理后台接口失败仍提示操作成功的问题
- 修复家长端返回固定概况的问题，改为聚合真实成长、任务和模考数据
- 修复成长数据月报返回占位字符串的问题
- 修复题目接口由本地硬编码样例生成的问题
- 修复定时任务方法体为空的问题
- 修复 `ExamServiceTest` 反射注入父类 `baseMapper` 失败的问题

### 变更
- 生产环境必须显式配置 `JWT_SECRET` 和 `AI_LLM_*`，缺失真实 AI 配置时返回明确错误
- 前端请求失败不再静默降级为 Mock 数据或假成功提示
- 模考等效分折算不再固定使用 0.85，改为按考试类型、总分区间和薄弱科目动态折算
- 院校匹配优先按位次窗口匹配，缺少位次数据时回退到批次匹配
- 成长记录在跨段位时自动写入 `growth_record`
- 前端成长月报兜底文案改为可交付总结，不再提示后续版本

### 移除
- 移除前端登录页任意账号 Mock 登录能力
- 移除管理后台、家长端、学生首页、模考中心、学习中心、成长页的 Mock 数据兜底

### 安全
- 强化三角色权限隔离：学生只能访问自身数据，家长只读绑定学生，管理员不进入学生答题写入路径

---

## [1.2.0] - 待发布

### 新增
- 多学生管理（家长端）
- 错题补强机制
- 周/月度复盘报告
- 多平台适配（PC端、移动端）

### 修复
- 暂无

### 变更
- 暂无

### 移除
- 暂无

### 安全
- 暂无
