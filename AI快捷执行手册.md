# 高中全科AI升学成长陪伴系统 - AI快捷执行手册

> 本手册包含全部任务卡片，供任何AI工具零上下文启动执行
> 技术栈：Vue3 + SpringBoot + MySQL + Redis
> 开发红线：零人工运维、仅稳妥院校对标、三角色权限隔离

---

## 一、执行说明

### 1.1 阅读顺序
1. 阅读本手册开头的执行说明和项目概述
2. 找到自己负责的任务模块
3. 查看任务依赖，确认前置任务已完成
4. 按照任务卡片中的验收标准执行
5. 完成后更新任务状态和执行日志

### 1.2 状态标记规则
- `[ ]` pending - 待开始
- `[*]` in_progress - 进行中
- `[x]` done - 完成
- `[✓]` audited - 已审计
- `[!]` blocked - 阻塞

### 1.3 执行日志格式
```
[2026-06-28 10:00:00] [Claude Code] START TASK-01 用户权限服务
[2026-06-28 12:30:00] [Claude Code] CREATE backend/src/main/java/com/example/sys/UserController.java
[2026-06-28 14:00:00] [Claude Code] COMPLETE TASK-01 用户权限服务
```

---

## 二、项目概述

### 2.1 核心定位
AI原生全自动学情陪伴 + 智能段位成长系统

### 2.2 三大核心能力
1. **AI全自动化**：题库生成、位次换算、院校匹配、任务生成全部AI驱动
2. **段位激励**：可视化展示当前段位与目标差距，量化激励
3. **零人工运维**：上线后无需人工维护任何数据

### 2.3 技术架构
```
前端展示层（Vue3）→ 接口网关层 → 业务逻辑层 → AI大模型中枢层 → 数据持久层 → 缓存层
```

### 2.4 三角色权限
- **学生**：学习答题、查看段位成长数据
- **家长**：只读监督，查看AI生成报告
- **管理员**：账号管理、系统监控，无数据运维权限

---

## 三、任务卡片列表

### 模块0：项目初始化（Trae负责）

#### TASK-INIT：项目初始化
**状态**: `[*]`  
**优先级**: high  
**负责AI**: Trae  
**依赖任务**: 无  
**输入依赖**: 无  
**输出产物**:
- `backend/pom.xml`
- `backend/src/main/java/com/example/SuperStudyApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/sql/init.sql`
- `backend/src/main/java/com/example/config/SecurityConfig.java`
- `backend/src/main/java/com/example/config/MyBatisPlusConfig.java`
- `backend/src/main/java/com/example/config/RedisConfig.java`
- `backend/src/main/java/com/example/config/GlobalExceptionHandler.java`
- `backend/src/main/java/com/example/config/BusinessException.java`
- `backend/src/main/java/com/example/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/example/util/JwtUtil.java`
- `backend/src/main/java/com/example/util/ResponseResult.java`
- `backend/src/main/java/com/example/dto/PageResult.java`
- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/tsconfig.node.json`
- `frontend/index.html`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/router/index.js`
- `frontend/src/utils/request.js`
- `frontend/src/utils/auth.js`
- `frontend/src/style/index.scss`
- 各页面基础占位组件

**功能描述**: 创建SpringBoot项目骨架、Vue3项目骨架、数据库初始化脚本、基础配置类和工具类

**技术要点**:
- SpringBoot 2.7.18 + MyBatis-Plus 3.5.3 + SpringSecurity 6
- Vue3 + Vite4 + Element Plus
- 数据库初始化SQL（9张核心表）
- JWT工具类和统一响应工具类
- 跨域配置和全局异常处理
- 前端路由、请求封装、权限管理

**验收标准**:
- [x] 后端项目骨架完整，结构清晰
- [x] 前端项目骨架完整，依赖安装成功
- [x] 前端开发服务器可正常启动
- [x] 数据库初始化脚本完整
- [x] JWT工具类编写完成
- [x] 统一响应格式正确
- [x] 跨域配置生效
- [x] 全局异常处理配置完成

**审计检查点**:
- [x] 项目目录结构符合规范
- [x] pom.xml依赖完整
- [x] application.yml配置正确
- [x] 数据库脚本完整
- [x] 工具类编写规范
- [x] 前端路由配置正确
- [x] 请求封装完善

**执行日志**:
- [2026-06-28 10:00:00] [Trae] START TASK-INIT 项目初始化
- [2026-06-28 10:15:00] [Trae] CREATE backend/pom.xml - SpringBoot依赖配置
- [2026-06-28 10:20:00] [Trae] CREATE SuperStudyApplication.java - 启动类
- [2026-06-28 10:25:00] [Trae] CREATE application.yml - 应用配置
- [2026-06-28 10:35:00] [Trae] CREATE SecurityConfig.java - 安全配置
- [2026-06-28 10:40:00] [Trae] CREATE JwtAuthenticationFilter.java - JWT认证过滤器
- [2026-06-28 10:45:00] [Trae] CREATE JwtUtil.java - JWT工具类
- [2026-06-28 10:50:00] [Trae] CREATE ResponseResult.java - 统一响应类
- [2026-06-28 10:55:00] [Trae] CREATE MyBatisPlusConfig.java - MyBatis-Plus配置
- [2026-06-28 11:00:00] [Trae] CREATE RedisConfig.java - Redis配置
- [2026-06-28 11:05:00] [Trae] CREATE GlobalExceptionHandler.java - 全局异常处理
- [2026-06-28 11:10:00] [Trae] CREATE BusinessException.java - 业务异常类
- [2026-06-28 11:15:00] [Trae] CREATE frontend/package.json - 前端依赖配置
- [2026-06-28 11:20:00] [Trae] CREATE vite.config.ts - Vite配置
- [2026-06-28 11:25:00] [Trae] CREATE tsconfig.json - TypeScript配置
- [2026-06-28 11:30:00] [Trae] CREATE main.ts / App.vue - 入口文件
- [2026-06-28 11:35:00] [Trae] CREATE router/index.js - 路由配置
- [2026-06-28 11:40:00] [Trae] CREATE utils/request.js - Axios封装
- [2026-06-28 11:45:00] [Trae] CREATE utils/auth.js - 权限工具
- [2026-06-28 11:50:00] [Trae] CREATE 各页面基础占位组件
- [2026-06-28 12:00:00] [Trae] VERIFY 前端依赖安装成功，开发服务器正常启动
- [2026-06-28 12:05:00] [Trae] COMPLETE TASK-INIT 项目初始化

---

### 模块1：AI大模型中枢层（扣子负责）

#### TASK-AI01：高考数据智能抓取接口
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: 扣子  
**依赖任务**: 无  
**输入依赖**: 无  
**输出产物**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- `backend/src/main/java/com/example/ai/crawler/GaokaoDataCrawler.java`
- `backend/src/main/resources/application.yml`（AI配置部分）

**功能描述**: 定时任务触发LLM，抓取各省教育考试院一分一段数据，清洗结构化写入score_rank表

**技术要点**:
- 使用Spring Scheduled定时触发（每日凌晨）
- 对接LLM API进行数据抓取和结构化
- 自动过滤重复、过期数据
- 异常处理和重试机制
- 支持物理/历史科类区分

**验收标准**:
- [ ] 每日自动抓取最新一分一段数据
- [ ] 数据正确写入score_rank表
- [ ] 自动过滤重复数据
- [ ] 异常时有重试机制和日志记录

**审计检查点**:
- [ ] 接口文档完整
- [ ] 定时任务配置正确
- [ ] 异常处理完善
- [ ] 数据清洗逻辑正确

**执行日志**:
- 

#### TASK-AI02：院校素材聚合接口
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: 扣子  
**依赖任务**: TASK-AI01  
**输入依赖**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`

**输出产物**:
- `backend/src/main/java/com/example/ai/CollegeService.java`
- `backend/src/main/java/com/example/ai/crawler/CollegeCrawler.java`
- `backend/src/main/resources/static/logos/`（院校LOGO存储目录）

**功能描述**: 按需/定时抓取院校官方LOGO、名称、批次，分类存入college_basic表

**技术要点**:
- 遵守robots协议，使用官方公开数据源
- 图片存储优化（压缩、CDN）
- 学生首页请求时根据批次随机取3条返回
- 支持物理/历史科类适配区分

**验收标准**:
- [ ] 院校数据正确写入college_basic表
- [ ] LOGO正确下载并存储
- [ ] 根据批次随机返回3所院校
- [ ] 支持物理/历史科类筛选

**审计检查点**:
- [ ] 数据源合规（官方公开）
- [ ] 图片存储路径配置正确
- [ ] 随机返回逻辑正确
- [ ] 数据更新机制完善

**执行日志**:
- 

#### TASK-AI03：智能题库生成接口
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: 扣子  
**依赖任务**: TASK-AI01  
**输入依赖**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`

**输出产物**:
- `backend/src/main/java/com/example/ai/QuestionBankService.java`
- `backend/src/main/java/com/example/ai/generator/QuestionGenerator.java`
- `backend/src/main/java/com/example/dto/QuestionDTO.java`

**功能描述**: 根据学生选科、当前分数赛道、薄弱知识点，实时生成对应难度习题

**技术要点**:
- 基于考纲生成题目的Prompt设计
- 难度分层逻辑：
  - ≤500分：60%基础 + 30%中档 + 10%简单压轴
  - 500-580分：均衡分配
  - ≥580分：增加难题占比
- 每周自动迭代题库，淘汰低适配题目
- 每道题打知识点、难度、适配分数段标签

**验收标准**:
- [ ] 根据学生分数赛道返回对应难度题目
- [ ] 题目正确写入ai_question_bank表
- [ ] 每周自动迭代淘汰老旧题目
- [ ] 题目包含知识点和难度标签

**审计检查点**:
- [ ] Prompt设计合理
- [ ] 难度分层逻辑正确
- [ ] 题库迭代机制完善
- [ ] AI生成质量可控

**执行日志**:
- 

#### TASK-AI04：学情分析&任务生成接口
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: 扣子  
**依赖任务**: TASK-AI01, TASK-AI03  
**输入依赖**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- `backend/src/main/java/com/example/ai/QuestionBankService.java`

**输出产物**:
- `backend/src/main/java/com/example/ai/LearningAnalysisService.java`
- `backend/src/main/java/com/example/dto/TaskDTO.java`
- `backend/src/main/java/com/example/dto/AnalysisDTO.java`

**功能描述**: 输入学生历次分数、错题集合，输出薄弱学科标签、每日学习时长权重、当日个性化任务清单

**技术要点**:
- 提分拆分权重算法：得分率<40%极弱学科分配40%时长
- 任务生成千人千面逻辑
- 周/月度复盘报告生成
- 学科权重分配：极弱40%、薄弱25%、优势15%-20%

**验收标准**:
- [ ] 正确识别薄弱学科
- [ ] 生成个性化每日任务清单
- [ ] 周/月度复盘报告自动生成
- [ ] 学科权重分配符合算法规则

**审计检查点**:
- [ ] 算法逻辑正确
- [ ] 任务生成多样性（千人千面）
- [ ] 复盘报告内容完整
- [ ] 与标杆案例（450→600）适配验证

**执行日志**:
- 

#### TASK-AI05：段位激励文案生成接口
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: 扣子  
**依赖任务**: TASK-AI01, TASK-AI02  
**输入依赖**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- `backend/src/main/java/com/example/ai/CollegeService.java`

**输出产物**:
- `backend/src/main/java/com/example/ai/IncentiveService.java`
- `backend/src/main/java/com/example/dto/IncentiveDTO.java`

**功能描述**: 输入当前分、目标分、心仪院校分差，动态生成极简激励短句、段位总结文案

**技术要点**:
- 避免固定话术库，确保多样性
- 正向激励导向，无压迫性预警
- 文案风格全局可配置
- 支持多种场景：段位升级、提分进步、成绩停滞

**验收标准**:
- [ ] 生成多样化激励文案
- [ ] 文案符合正向激励导向
- [ ] 支持段位升级、提分、停滞三种场景
- [ ] 文案风格可配置

**审计检查点**:
- [ ] 文案多样性（不重复）
- [ ] 无负面/压迫性语言
- [ ] 场景覆盖完整
- [ ] 配置接口完整

**执行日志**:
- 

---

### 模块2：后端业务服务（Claude Code负责）

#### TASK-BE01：用户权限服务
**状态**: `[x]`  
**优先级**: high  
**负责AI**: Claude Code  
**依赖任务**: 无  
**输入依赖**: 无  
**输出产物**:
- `backend/src/main/java/com/example/sys/UserController.java`
- `backend/src/main/java/com/example/sys/UserService.java`
- `backend/src/main/java/com/example/sys/UserServiceImpl.java`
- `backend/src/main/java/com/example/sys/entity/SysUser.java`
- `backend/src/main/java/com/example/sys/mapper/SysUserMapper.java`
- `backend/src/main/java/com/example/config/SecurityConfig.java`
- `backend/src/main/java/com/example/config/JwtConfig.java`
- `backend/src/main/java/com/example/util/JwtUtil.java`

**功能描述**: JWT登录鉴权、单端登录校验、角色接口拦截、学生档案基础CRUD

**技术要点**:
- SpringSecurity 6配置
- JWT Token生成和验证
- 权限注解控制（@PreAuthorize）
- 单端登录限制（Redis存储Token）
- 三角色权限隔离

**验收标准**:
- [ ] 学生/家长/管理员三角色登录成功
- [ ] JWT Token正确生成和验证
- [ ] 单端登录限制生效
- [ ] 角色权限拦截正确（家长不可答题、管理员不可修改学情）

**审计检查点**:
- [ ] SpringSecurity配置完整
- [ ] JWT逻辑正确
- [ ] 权限注解使用正确
- [ ] 密码加密存储（BCrypt）

**执行日志**:
- 

#### TASK-BE02：学生档案服务
**状态**: `[x]`  
**优先级**: high  
**负责AI**: Claude Code  
**依赖任务**: TASK-BE01  
**输入依赖**:
- `backend/src/main/java/com/example/sys/UserService.java`

**输出产物**:
- `backend/src/main/java/com/example/student/StudentController.java`
- `backend/src/main/java/com/example/student/StudentService.java`
- `backend/src/main/java/com/example/student/entity/StudentProfile.java`
- `backend/src/main/java/com/example/student/mapper/StudentProfileMapper.java`

**功能描述**: 学生档案基础信息CRUD，年级/选科/目标分等基础信息管理

**技术要点**:
- 基础信息仅可手动录入一次，后续AI自动更新
- 备考倒计时AI自动计算
- 目标总分、心仪院校手动录入
- 基线学情AI自动诊断

**验收标准**:
- [ ] 学生档案CRUD功能完整
- [ ] 备考倒计时自动计算
- [ ] 基础信息录入后锁定（不可重复修改）
- [ ] 与用户权限服务集成

**审计检查点**:
- [ ] 数据校验完善
- [ ] 锁定机制正确
- [ ] 与用户表关联正确
- [ ] 接口权限控制完整

**执行日志**:
- 

#### TASK-BE03：模考位次换算服务
**状态**: `[x]`  
**优先级**: high  
**负责AI**: Claude Code  
**依赖任务**: TASK-BE01, TASK-BE02, TASK-AI01, TASK-AI02  
**输入依赖**:
- `backend/src/main/java/com/example/student/StudentService.java`
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- `backend/src/main/java/com/example/ai/CollegeService.java`

**输出产物**:
- `backend/src/main/java/com/example/exam/ExamController.java`
- `backend/src/main/java/com/example/exam/ExamService.java`
- `backend/src/main/java/com/example/exam/entity/ExamRecord.java`
- `backend/src/main/java/com/example/exam/mapper/ExamRecordMapper.java`

**功能描述**: 考试提交后自动折算等效高考总分、匹配近三年等效位次、匹配院校批次

**技术要点**:
- 位次换算算法（分数↔位次双向映射）
- 稳妥院校匹配阈值：等效位次±5000名内同批次院校
- 随机抽取逻辑（每次请求打乱顺序）
- 校内试卷难度加权折算

**验收标准**:
- [ ] 考试分数正确折算为等效高考分
- [ ] 正确匹配近三年等效位次
- [ ] 随机返回3所同批次稳妥院校
- [ ] 计算当前分与目标分差距

**审计检查点**:
- [ ] 位次换算算法正确
- [ ] 院校匹配逻辑符合规则
- [ ] 随机抽取实现正确
- [ ] 与标杆案例验证通过

**执行日志**:
- 

#### TASK-BE04：AI自适应学习服务
**状态**: `[x]`  
**优先级**: high  
**负责AI**: Claude Code  
**依赖任务**: TASK-BE01, TASK-BE02, TASK-AI03, TASK-AI04  
**输入依赖**:
- `backend/src/main/java/com/example/student/StudentService.java`
- `backend/src/main/java/com/example/ai/QuestionBankService.java`
- `backend/src/main/java/com/example/ai/LearningAnalysisService.java`

**输出产物**:
- `backend/src/main/java/com/example/learning/LearningController.java`
- `backend/src/main/java/com/example/learning/LearningService.java`
- `backend/src/main/java/com/example/learning/entity/TaskRecord.java`
- `backend/src/main/java/com/example/learning/entity/ErrorQuestion.java`
- `backend/src/main/java/com/example/learning/mapper/TaskRecordMapper.java`
- `backend/src/main/java/com/example/learning/mapper/ErrorQuestionMapper.java`

**功能描述**: 每日任务生成、错题归集、补强推送、周/月复盘报告

**技术要点**:
- 学科权重AI分配算法
- 错题智能归类和补强逻辑
- 定时任务调度（每日任务生成）
- 任务完成率统计

**验收标准**:
- [ ] 每日自动生成个性化任务
- [ ] 错题自动归集
- [ ] 同类补强习题推送
- [ ] 周/月度复盘报告自动生成

**审计检查点**:
- [ ] 任务生成逻辑正确
- [ ] 错题归集完善
- [ ] 补强推送机制合理
- [ ] 统计数据准确

**执行日志**:
- 

#### TASK-BE05：段位激励服务
**状态**: `[x]`  
**优先级**: medium  
**负责AI**: Claude Code  
**依赖任务**: TASK-BE02, TASK-BE03, TASK-AI02, TASK-AI05  
**输入依赖**:
- `backend/src/main/java/com/example/student/StudentService.java`
- `backend/src/main/java/com/example/exam/ExamService.java`
- `backend/src/main/java/com/example/ai/CollegeService.java`
- `backend/src/main/java/com/example/ai/IncentiveService.java`

**输出产物**:
- `backend/src/main/java/com/example/growth/GrowthController.java`
- `backend/src/main/java/com/example/growth/GrowthService.java`
- `backend/src/main/java/com/example/growth/entity/GrowthRecord.java`
- `backend/src/main/java/com/example/growth/mapper/GrowthRecordMapper.java`

**功能描述**: 院校卡片统一返回、段位进度条数据、段位升级记录、心仪院校分差计算

**技术要点**:
- 缓存优化（Redis缓存院校素材）
- 实时计算性能优化
- 段位升级动效数据准备
- 成长记录持久化

**验收标准**:
- [ ] 院校卡片统一结构返回
- [ ] 段位进度条数据正确
- [ ] 段位升级记录保存
- [ ] 心仪院校分差实时计算

**审计检查点**:
- [ ] 缓存策略合理
- [ ] 计算性能优化
- [ ] 数据结构统一
- [ ] 升级记录完整

**执行日志**:
- 

#### TASK-BE06：定时自治任务
**状态**: `[x]`  
**优先级**: medium  
**负责AI**: Claude Code  
**依赖任务**: TASK-BE01, TASK-AI01, TASK-AI02, TASK-AI03  
**输入依赖**:
- `backend/src/main/java/com/example/ai/GaokaoDataService.java`
- `backend/src/main/java/com/example/ai/CollegeService.java`
- `backend/src/main/java/com/example/ai/QuestionBankService.java`

**输出产物**:
- `backend/src/main/java/com/example/task/ScheduledTasks.java`
- `backend/src/main/resources/application.yml`（定时任务配置）

**功能描述**: 每日凌晨AI同步位次数据、每周刷新院校素材和迭代题库、每日MySQL自动备份

**技术要点**:
- Spring Scheduled配置
- 任务执行日志记录
- 异常告警机制
- 数据库备份脚本集成

**验收标准**:
- [ ] 每日凌晨自动同步位次数据
- [ ] 每周自动刷新院校素材和题库
- [ ] 每日自动备份MySQL
- [ ] 任务异常时有告警和日志

**审计检查点**:
- [ ] 定时任务配置正确
- [ ] 日志记录完整
- [ ] 异常处理完善
- [ ] 备份机制可靠

**执行日志**:
- 

---

### 模块3：前端页面开发（Cursor负责）

#### TASK-FE01：登录统一页面
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: Cursor  
**依赖任务**: TASK-INIT, TASK-BE01  
**输入依赖**:
- `backend/src/main/java/com/example/sys/UserController.java`
- `frontend/src/utils/auth.js`
- `frontend/src/router/index.js`

**输出产物**:
- `frontend/src/views/login/index.vue`
- `frontend/src/api/user.js`（登录接口）
- `frontend/src/utils/auth.js`（Token管理）

**功能描述**: 三角色共用登录表单，JWT令牌登录，角色自动识别跳转

**技术要点**:
- 前端路由权限控制
- Token存储和管理
- 验证码防刷机制
- 无注册、游客入口

**验收标准**:
- [ ] 学生/家长/管理员三角色登录成功
- [ ] 登录后根据角色跳转对应页面
- [ ] Token正确存储和携带
- [ ] 单端登录限制生效

**审计检查点**:
- [ ] 路由权限控制完整
- [ ] Token管理安全
- [ ] 页面UI符合设计规范
- [ ] 无注册入口（开发红线）

**执行日志**:
- 

#### TASK-FE02：学生首页（核心页面）
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE02, TASK-BE03, TASK-BE05, TASK-AI02, TASK-AI05  
**输入依赖**:
- `backend/src/main/java/com/example/growth/GrowthController.java`
- `backend/src/main/java/com/example/learning/LearningController.java`
- `backend/src/main/java/com/example/ai/IncentiveService.java`

**输出产物**:
- `frontend/src/views/student/home/index.vue`
- `frontend/src/components/CollegeCard.vue`（院校卡片组件）
- `frontend/src/components/GrowthProgress.vue`（段位进度条组件）
- `frontend/src/api/growth.js`
- `frontend/src/api/learning.js`

**功能描述**: 顶部状态栏、三段式AI院校对标卡片、段位进度条、AI今日任务、AI今日点评

**技术要点**:
- ECharts动态渲染进度条
- 院校卡片固定组件封装（LOGO + 校名 + 批次）
- 段位升级动效（高亮渐变、轻微缩放、弹窗激励）
- AI加载动效（光点轻量化动画）

**验收标准**:
- [ ] 高考倒计时实时更新
- [ ] 三段式院校卡片正确展示
- [ ] 段位进度条动态渲染
- [ ] AI今日任务和点评展示
- [ ] 段位升级动效触发

**审计检查点**:
- [ ] 院校卡片统一样式
- [ ] 进度条动画流畅
- [ ] AI加载动效符合规范
- [ ] 页面响应式适配

**执行日志**:
- 

#### TASK-FE03：AI智能学习中心
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE04, TASK-AI03  
**输入依赖**:
- `backend/src/main/java/com/example/learning/LearningController.java`
- `backend/src/main/java/com/example/ai/QuestionBankService.java`

**输出产物**:
- `frontend/src/views/student/learning/index.vue`
- `frontend/src/components/QuestionCard.vue`（题目卡片组件）
- `frontend/src/components/KnowledgeHeatmap.vue`（知识点热力图组件）
- `frontend/src/api/question.js`

**功能描述**: AI个性化专项训练区、AI错题智能复盘区、知识点掌握图谱、每日打卡记录

**技术要点**:
- 题目实时从AI接口获取，无手动导入按钮
- 热力图ECharts实现
- 错题自动归集和补强推送
- 打卡记录统计展示

**验收标准**:
- [ ] AI专项训练题目展示
- [ ] 错题复盘功能完整
- [ ] 知识点热力图渲染
- [ ] 打卡记录统计

**审计检查点**:
- [ ] 无题库导入按钮（开发红线）
- [ ] 热力图数据准确
- [ ] 错题归集逻辑正确
- [ ] 页面交互流畅

**执行日志**:
- 

#### TASK-FE04：AI全真模考中心
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE03, TASK-AI03  
**输入依赖**:
- `backend/src/main/java/com/example/exam/ExamController.java`
- `backend/src/main/java/com/example/ai/QuestionBankService.java`

**输出产物**:
- `frontend/src/views/student/exam/index.vue`
- `frontend/src/components/ExamPaper.vue`（试卷组件）
- `frontend/src/components/ExamReport.vue`（诊断报告组件）
- `frontend/src/api/exam.js`

**功能描述**: 周测/月度模考入口、历史考试列表、考后AI诊断报告

**技术要点**:
- 试卷由LLM实时生成
- 考完自动调用位次换算接口刷新段位
- 诊断报告包含得分率、薄弱点、段位升降
- 在线答题交互

**验收标准**:
- [ ] 周测/月度模考入口展示
- [ ] 历史考试记录列表
- [ ] 在线答题功能完整
- [ ] 考后AI诊断报告展示

**审计检查点**:
- [ ] 试卷生成逻辑正确
- [ ] 位次自动更新
- [ ] 诊断报告内容完整
- [ ] 答题交互流畅

**执行日志**:
- 

#### TASK-FE05：AI成长数据中心
**状态**: `[ ]`  
**优先级**: high  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE03, TASK-BE05  
**输入依赖**:
- `backend/src/main/java/com/example/growth/GrowthController.java`
- `backend/src/main/java/com/example/exam/ExamController.java`

**输出产物**:
- `frontend/src/views/student/growth/index.vue`
- `frontend/src/components/ScoreChart.vue`（分数曲线组件）
- `frontend/src/components/RankChart.vue`（位次曲线组件）
- `frontend/src/components/GrowthHistory.vue`（升级历史组件）

**功能描述**: 总分趋势、位次曲线、单科曲线、段位升级历史、AI月度报告

**技术要点**:
- ECharts多图表联动
- 数据全部由AI统计服务输出
- 可视化动态更新
- 报告展示

**验收标准**:
- [ ] 总分趋势曲线渲染
- [ ] 位次波动曲线渲染
- [ ] 单科提分曲线渲染
- [ ] 段位升级历史展示
- [ ] AI月度报告展示

**审计检查点**:
- [ ] 图表数据准确
- [ ] 多图表联动流畅
- [ ] 数据可视化美观
- [ ] 报告格式规范

**执行日志**:
- 

#### TASK-FE06：家长端页面（纯只读）
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE02, TASK-BE03, TASK-BE04  
**输入依赖**:
- `backend/src/main/java/com/example/student/StudentService.java`
- `backend/src/main/java/com/example/exam/ExamService.java`
- `backend/src/main/java/com/example/learning/LearningService.java`

**输出产物**:
- `frontend/src/views/parent/index.vue`
- `frontend/src/api/parent.js`

**功能描述**: 当前段位展示、心仪院校分差、本周AI点评、学习完成率、成绩趋势

**技术要点**:
- 严格禁止任何编辑、提交、上传按钮
- 极简数据展示，无繁杂数据
- 只读权限控制

**验收标准**:
- [ ] 当前段位展示
- [ ] 心仪院校分差展示
- [ ] 本周AI点评展示
- [ ] 学习完成率统计
- [ ] 成绩趋势展示

**审计检查点**:
- [ ] 无任何编辑按钮（开发红线）
- [ ] 数据展示极简
- [ ] 权限控制正确
- [ ] 页面响应式适配

**执行日志**:
- 

#### TASK-FE07：管理员后台（AI Native精简版）
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Cursor  
**依赖任务**: TASK-FE01, TASK-BE01, TASK-BE06  
**输入依赖**:
- `backend/src/main/java/com/example/sys/UserController.java`
- `backend/src/main/java/com/example/task/ScheduledTasks.java`

**输出产物**:
- `frontend/src/views/admin/user/index.vue`（用户管理页）
- `frontend/src/views/admin/monitor/index.vue`（系统监控页）
- `frontend/src/api/admin.js`

**功能描述**: 用户账号管理、系统运行日志、自动备份状态、AI参数配置、接口状态监控

**技术要点**:
- 彻底删除题库管理、院校素材管理、高考数据录入等页面
- 只保留账号管理和系统监控
- 权限控制严格

**验收标准**:
- [ ] 用户账号新增/禁用/重置功能
- [ ] 系统运行日志展示
- [ ] 自动备份状态展示
- [ ] AI参数配置功能
- [ ] 接口状态监控

**审计检查点**:
- [ ] 无题库管理页面（开发红线）
- [ ] 无院校管理页面（开发红线）
- [ ] 权限控制完整
- [ ] 页面符合极简设计

**执行日志**:
- 

---

### 模块4：测试与优化（Codex负责）

#### TASK-TS01：单元测试编写
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Codex  
**依赖任务**: TASK-BE01~TASK-BE06  
**输入依赖**:
- `backend/src/main/java/com/example/sys/UserService.java`
- `backend/src/main/java/com/example/exam/ExamService.java`
- `backend/src/main/java/com/example/learning/LearningService.java`

**输出产物**:
- `backend/src/test/java/com/example/sys/UserServiceTest.java`
- `backend/src/test/java/com/example/exam/ExamServiceTest.java`
- `backend/src/test/java/com/example/learning/LearningServiceTest.java`
- `backend/src/test/java/com/example/ai/GaokaoDataServiceTest.java`

**功能描述**: 编写核心业务逻辑单元测试，确保覆盖率≥80%

**技术要点**:
- Mock测试框架（Mockito）
- 测试用例覆盖正向和异常场景
- 标杆案例（450→600）测试验证
- 测试报告生成

**验收标准**:
- [ ] 单元测试覆盖率≥80%
- [ ] 所有测试用例通过
- [ ] 标杆案例测试验证通过
- [ ] 测试报告生成

**审计检查点**:
- [ ] 测试覆盖率达标
- [ ] 测试用例质量高
- [ ] 异常场景覆盖
- [ ] 测试报告完整

**执行日志**:
- 

#### TASK-TS02：集成测试编写
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Codex  
**依赖任务**: TASK-BE01~TASK-BE06, TASK-TS01  
**输入依赖**:
- 所有后端Controller接口

**输出产物**:
- `backend/src/test/java/com/example/integration/UserIntegrationTest.java`
- `backend/src/test/java/com/example/integration/ExamIntegrationTest.java`
- `backend/src/test/java/com/example/integration/LearningIntegrationTest.java`

**功能描述**: 编写API接口集成测试，验证端到端流程

**技术要点**:
- Spring Boot Test框架
- 数据库Mock（H2内存数据库）
- 接口权限测试
- 全流程测试

**验收标准**:
- [ ] 所有API接口集成测试通过
- [ ] 权限控制测试通过
- [ ] 端到端流程测试通过
- [ ] 接口返回格式符合规范

**审计检查点**:
- [ ] 集成测试覆盖完整
- [ ] 权限测试充分
- [ ] 数据一致性验证
- [ ] 测试环境配置正确

**执行日志**:
- 

#### TASK-TS03：性能优化与Bug修复
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Codex  
**依赖任务**: TASK-BE01~TASK-BE06, TASK-TS01, TASK-TS02  
**输入依赖**:
- 测试报告
- 性能监控数据

**输出产物**:
- 优化后的代码文件
- Bug修复记录
- 性能测试报告

**功能描述**: 根据测试结果进行性能优化和Bug修复

**技术要点**:
- 数据库查询优化（慢查询分析）
- 代码重构优化
- Bug定位和修复
- 性能测试验证

**验收标准**:
- [ ] 所有已知Bug修复
- [ ] 数据库查询性能优化
- [ ] 接口响应时间达标
- [ ] 性能测试通过

**审计检查点**:
- [ ] Bug修复完整
- [ ] 性能提升明显
- [ ] 代码质量提高
- [ ] 测试验证通过

**执行日志**:
- 

---

### 模块5：部署运维（WorkBuddy负责）

#### TASK-OP01：Docker部署配置
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: WorkBuddy  
**依赖任务**: TASK-BE01~TASK-BE06, TASK-FE01~TASK-FE07  
**输入依赖**:
- 后端项目源码
- 前端项目源码

**输出产物**:
- `docker-compose.yml`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `nginx.conf`
- `.env`（环境变量配置）

**功能描述**: 配置Docker容器化部署，支持Linux云服务器一键部署

**技术要点**:
- Docker多阶段构建
- Nginx反向代理配置
- HTTPS证书配置
- 环境变量分离

**验收标准**:
- [ ] Docker镜像构建成功
- [ ] docker-compose一键启动
- [ ] Nginx配置正确
- [ ] HTTPS配置完成

**审计检查点**:
- [ ] Dockerfile优化
- [ ] 环境变量安全
- [ ] 容器健康检查
- [ ] 部署文档完整

**执行日志**:
- 

#### TASK-OP02：数据库备份脚本
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: WorkBuddy  
**依赖任务**: TASK-BE06  
**输入依赖**:
- `backend/src/main/java/com/example/task/ScheduledTasks.java`

**输出产物**:
- `scripts/backup.sh`（数据库备份脚本）
- `scripts/restore.sh`（数据库恢复脚本）
- `scripts/backup_cron`（定时任务配置）

**功能描述**: 配置每日MySQL全量自动备份，保留30天备份记录

**技术要点**:
- mysqldump命令备份
- 压缩存储优化
- 自动清理过期备份
- 备份状态监控

**验收标准**:
- [ ] 每日自动备份执行
- [ ] 备份文件压缩存储
- [ ] 30天自动清理
- [ ] 备份状态可监控

**审计检查点**:
- [ ] 备份脚本完整
- [ ] 恢复脚本测试通过
- [ ] 存储策略合理
- [ ] 异常处理完善

**执行日志**:
- 

#### TASK-OP03：监控告警配置
**状态**: `[ ]`  
**优先级**: low  
**负责AI**: WorkBuddy  
**依赖任务**: TASK-OP01, TASK-OP02  
**输入依赖**:
- Docker部署配置
- 备份脚本

**输出产物**:
- `monitoring/prometheus.yml`（可选）
- `monitoring/grafana/dashboards/`（可选）
- `scripts/health_check.sh`（健康检查脚本）
- `scripts/alert.sh`（告警脚本）

**功能描述**: 配置系统运行状态监控、接口健康检查、AI任务状态监控

**技术要点**:
- 接口健康检查
- 日志监控
- 异常告警机制
- 性能指标收集

**验收标准**:
- [ ] 系统运行状态监控
- [ ] 接口健康检查
- [ ] AI任务状态监控
- [ ] 异常告警触发

**审计检查点**:
- [ ] 监控覆盖全面
- [ ] 告警机制有效
- [ ] 指标收集完整
- [ ] 配置文档完整

**执行日志**:
- 

---

### 模块6：全栈集成（Trae负责）

#### TASK-IT01：接口联调与端到端测试
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Trae  
**依赖任务**: TASK-BE01~TASK-BE06, TASK-FE01~TASK-FE07  
**输入依赖**:
- 所有后端API接口
- 所有前端页面

**输出产物**:
- 联调测试报告
- 问题修复记录
- 端到端测试用例

**功能描述**: 前后端接口联调，端到端流程测试，问题排查和修复

**技术要点**:
- API接口对接测试
- 前端页面联调
- 端到端流程验证
- 问题定位和修复

**验收标准**:
- [ ] 所有接口联调通过
- [ ] 端到端流程测试通过
- [ ] 标杆案例全流程验证通过
- [ ] 所有已知问题修复

**审计检查点**:
- [ ] 接口对接完整
- [ ] 数据流转正确
- [ ] 用户体验流畅
- [ ] 测试报告完整

**执行日志**:
- 

#### TASK-IT02：上线前最终验证
**状态**: `[ ]`  
**优先级**: medium  
**负责AI**: Trae  
**依赖任务**: TASK-IT01, TASK-TS01~TASK-TS03, TASK-OP01~TASK-OP03  
**输入依赖**:
- 联调测试报告
- 测试报告
- 部署配置

**输出产物**:
- 上线验证报告
- 最终交付物清单确认

**功能描述**: 上线前全面验证，确保系统完整可用

**技术要点**:
- 功能完整性验证
- 性能验证
- 安全合规检查
- 开发红线检查

**验收标准**:
- [ ] 所有功能验证通过
- [ ] 性能指标达标
- [ ] 安全合规检查通过
- [ ] 开发红线检查通过

**审计检查点**:
- [ ] 开发红线合规
- [ ] 数据安全合规
- [ ] 功能完整
- [ ] 文档齐全

**执行日志**:
- 

---

## 四、审计规划

### 4.1 审计阶段划分

| 阶段 | 时间点 | 审计内容 | 审计方式 |
|-----|-------|---------|---------|
| 代码审计 | 每个任务完成后 | 代码质量、安全合规、规范遵守 | 自动扫描 + 人工抽检 |
| 模块审计 | 每个模块完成后 | 模块功能完整性、接口正确性、测试覆盖率 | 集成测试 + 代码审查 |
| 阶段审计 | 每个迭代完成后 | 阶段目标达成、功能完整性、用户体验 | 端到端测试 + 人工评审 |
| 上线审计 | 上线前 | 安全合规、性能指标、开发红线遵守 | 全面检查 + 安全扫描 |

### 4.2 代码审计检查清单

**后端代码审计**:
- [ ] 代码风格符合阿里巴巴Java开发手册
- [ ] 接口返回格式符合统一规范（{code, message, data}）
- [ ] 异常处理完善，无空指针风险
- [ ] 数据库查询使用索引，无慢查询
- [ ] 权限校验完整，无越权访问风险
- [ ] 关键业务逻辑有单元测试
- [ ] 无硬编码密钥或敏感信息
- [ ] SQL语句防止注入攻击

**前端代码审计**:
- [ ] 代码风格符合Vue3官方风格指南
- [ ] 组件封装合理，无冗余代码
- [ ] 响应式设计适配移动端
- [ ] API调用有错误处理和加载状态
- [ ] 权限路由控制完整
- [ ] 动效流畅，无卡顿
- [ ] 无硬编码敏感信息
- [ ] XSS防护措施

**AI模块审计**:
- [ ] LLM调用有重试机制和降级方案
- [ ] AI输出有质量校验和过滤
- [ ] Prompt工程合理，无歧义
- [ ] API密钥安全存储，不硬编码
- [ ] AI生成内容符合业务规则
- [ ] 数据抓取合规（官方公开数据源）

### 4.3 开发红线合规审计

| 红线项 | 审计方式 | 通过标准 |
|-------|---------|---------|
| 无人工上传入口 | 搜索所有页面 | 无"上传"、"导入"、"提交素材"按钮 |
| 无题库管理后台 | 检查管理后台页面 | 无题库管理页面 |
| 无院校素材上传 | 检查管理后台页面 | 无院校管理页面 |
| 仅稳妥院校对标 | 检查前端院校卡片逻辑 | 仅展示同批次稳妥院校，无冲保院校 |
| 三角色权限隔离 | 测试各角色接口访问 | 家长不可答题、管理员不可修改学情 |
| 零人工运维 | 检查所有后台功能 | 所有数据更新由AI自动完成 |
| AI自动生成 | 检查所有报告/文案 | 无固定话术库，100%AI生成 |

### 4.4 安全合规审计

| 审计项 | 标准 | 检查方式 |
|-------|------|---------|
| 密码加密 | BCrypt加密存储 | 检查数据库密码字段 |
| JWT安全 | Token有效期控制、单端登录 | 测试登录验证流程 |
| HTTPS | 全站HTTPS传输 | 检查部署配置 |
| SQL注入防护 | 使用MyBatis-Plus参数化查询 | 代码审查 |
| XSS防护 | 前端输入过滤、后端输出转义 | 安全扫描 |
| 数据加密 | 学情敏感数据加密存储 | 检查数据库字段 |
| 接口防刷 | 请求频率限制 | 测试API接口 |

### 4.5 性能审计

| 审计项 | 标准 | 检查方式 |
|-------|------|---------|
| 接口响应时间 | < 2秒（AI接口除外） | 性能测试 |
| AI接口响应时间 | < 5秒（95%请求） | 性能测试 |
| 数据库查询 | < 100ms（95%请求） | 慢查询日志分析 |
| 前端首屏加载 | < 2秒 | 页面加载测试 |
| 并发处理 | 支持100并发用户 | 压力测试 |

### 4.6 审计记录格式

```
审计ID: AUDIT-XX
审计类型: [代码审计/模块审计/阶段审计/上线审计]
审计时间: [时间戳]
审计范围: [任务ID/模块名/阶段名]
审计结果: [通过/不通过/待修复]
问题清单:
  - [问题描述] - [严重程度] - [修复建议]
审计人: [AI工具/人工]
```

---

## 五、执行流程

### 5.1 AI接手任务流程

```
1. 阅读《AI快捷执行手册》，了解项目概述和技术栈
2. 查找自己负责的任务模块
3. 确认任务依赖已完成（状态为done或audited）
4. 将任务状态更新为in_progress
5. 记录执行日志（START）
6. 按照任务卡片中的技术要点和验收标准执行
7. 完成后自测，验证输出产物
8. 记录执行日志（CREATE/MODIFY/COMPLETE）
9. 将任务状态更新为done
10. 通知下一任务负责人（如有）
```

### 5.2 任务执行规范

**文件命名规范**:
- Java类名：大驼峰命名（UserService.java）
- Vue组件名：大驼峰命名（CollegeCard.vue）
- 数据库表名：小写蛇形命名（sys_user）
- API接口路径：小写蛇形命名（/api/user/login）

**代码风格规范**:
- Java：遵循阿里巴巴Java开发手册
- Vue：遵循Vue3官方风格指南
- SQL：关键字大写，表名/字段名小写

**Git提交规范**:
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- refactor: 重构代码
- test: 测试相关
- chore: 构建/部署

### 5.3 API接口规范

**统一返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**错误码定义**:
| 错误码 | 含义 |
|-------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证（Token失效） |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

**接口版本控制**:
- `/api/v1/user/login`

### 5.4 数据库设计参考

**用户私有数据表**:
- sys_user - 用户账号表
- student_profile - 学生档案表
- exam_record - 考试记录表
- task_record - AI每日任务打卡表
- error_question - 错题归集表
- growth_record - 段位升级记录表

**AI自治资源数据表**:
- college_basic - 院校素材自治表
- score_rank - 一分一段自治表
- ai_question_bank - AI动态题库表

---

## 六、开发红线（强制执行）

### 6.1 禁止开发功能
1. ❌ 所有"上传、录入、编辑、提交素材"入口
2. ❌ 题库管理后台
3. ❌ 院校素材上传功能
4. ❌ 高考数据录入功能
5. ❌ 考点维护功能
6. ❌ 家长端编辑、提交、上传按钮
7. ❌ 管理员修改学情、题库、院校资源功能

### 6.2 强制实现功能
1. ✅ 所有院校、题库、报告、文案100% AI自动生成
2. ✅ 严格保留"仅稳妥院校"对标，无冲保院校
3. ✅ 核心聚焦学生段位认知、差距量化、自主激励
4. ✅ 三角色权限强隔离
5. ✅ 段位升级动效和激励文案
6. ✅ AI每日任务千人千面
7. ✅ 零人工运维设计理念

### 6.3 技术约束
1. ✅ 技术栈固定：Vue3 + SpringBoot + MySQL + Redis
2. ✅ 不得引入未批准的技术组件
3. ✅ 必须遵循RESTful API规范
4. ✅ 必须使用JWT鉴权

---

## 七、参考文档

1. [系统开发计划.md](系统开发计划.md) - 完整开发计划
2. [多AI协作开发总纲.md](多AI协作开发总纲.md) - 协作规则和角色分工
3. [高中全科AI升学成长陪伴系统 · 全套UI界面设计规范与页面开发手册（最终交付版）.md](高中全科AI升学成长陪伴系统 · 全套UI界面设计规范与页面开发手册（最终交付版）.md) - UI设计规范
4. [高中全科AI升学成长陪伴系统 完整落地开发执行方案.md](高中全科AI升学成长陪伴系统 完整落地开发执行方案.md) - 技术架构和实现方案

---

**文档版本**: v1.0  
**最后更新**: 2026-06-28  
**适用范围**: 所有参与本项目开发的AI工具

> 注：本手册为AI快捷执行手册，包含全部任务卡片和审计规划。任何AI工具接手任务时，仅需阅读本手册即可开始工作。