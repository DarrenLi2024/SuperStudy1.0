# 高中全科AI升学成长陪伴系统

> AI原生全自动学情陪伴 + 智能段位成长系统

---

## 🌟 核心定位

面向高中生的AI原生学习陪伴系统，实现：
- **AI全自动化**：题库生成、位次换算、院校匹配、任务生成全部AI驱动
- **段位激励**：可视化展示当前段位与目标差距，量化激励
- **零人工运维**：上线后无需人工维护任何数据

---

## ✨ 核心特性

| 特性 | 描述 |
|-----|------|
| 🤖 AI智能题库 | LLM实时生成、迭代更新高中全科题库 |
| 📊 模考位次换算 | 考试分数↔等效高考分↔等效位次双向映射 |
| 🏛️ 智能院校匹配 | 随机抽取3所同批次稳妥院校对标 |
| 📋 个性化任务 | AI分析学情，生成千人千面学习任务 |
| 🎯 段位成长系统 | 可视化段位进度，AI激励文案推送 |
| 📱 三端协同 | 学生端、家长端、管理后台 |
| 🚀 零人工运维 | 数据全自动更新，无人工录入入口 |

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|-----|------|-----|
| 前端 | Vue3 + Vite | 3.4.x + 4.5.x |
| UI组件 | Element Plus + Vant4 | 2.x + 4.x |
| 后端 | SpringBoot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 6.2+ |
| 认证 | JWT + SpringSecurity | 6.x |
| 部署 | Docker | 24.x |

---

## 📁 项目结构

```
SuperStudy1.0/
├── backend/                    # 后端服务
│   ├── src/main/java/com/example/
│   │   ├── ai/                # AI大模型中枢层
│   │   ├── sys/               # 用户权限服务
│   │   ├── student/           # 学生档案服务
│   │   ├── exam/              # 模考位次服务
│   │   ├── learning/          # AI自适应学习服务
│   │   ├── growth/            # 段位激励服务
│   │   ├── config/            # 配置类
│   │   ├── util/              # 工具类
│   │   ├── dto/               # 数据传输对象
│   │   └── entity/            # 实体类
│   └── src/main/resources/
│       ├── sql/               # 数据库脚本
│       └── application.yml    # 应用配置
├── frontend/                   # 前端应用
│   ├── src/
│   │   ├── views/             # 页面视图
│   │   ├── components/        # 公共组件
│   │   ├── api/               # API接口
│   │   ├── router/            # 路由配置
│   │   ├── utils/             # 工具函数
│   │   └── stores/            # 状态管理
│   └── package.json
├── scripts/                   # 自动化脚本
├── monitoring/                # 监控配置
└── docs/                      # 文档目录
```

---

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- MySQL 8.0+
- Redis 6.2+
- Node.js 18+

### 1. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS superstudy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE superstudy;
SOURCE backend/src/main/resources/sql/init.sql;
```

### 2. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

### 3. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问地址

| 角色 | 地址 | 账号/密码 |
|-----|------|----------|
| 学生端 | http://localhost:5173 | 学生手机号/123456 |
| 家长端 | http://localhost:5173/#/parent | 家长手机号/123456 |
| 管理后台 | http://localhost:5173/#/admin | admin/admin123 |

---

## 📚 核心文档

| 文档 | 说明 |
|-----|------|
| [系统开发计划.md](系统开发计划.md) | 完整开发计划与技术架构 |
| [多AI协作开发总纲.md](多AI协作开发总纲.md) | 多AI协作规则与分工策略 |
| [AI快捷执行手册.md](AI快捷执行手册.md) | 全部任务卡片与执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构与命名规范 |
| [API接口契约规范.md](API接口契约规范.md) | 接口定义与Mock数据策略 |
| [迭代任务映射表.md](迭代任务映射表.md) | 迭代计划与任务分配 |

---

## 👥 开发团队

本项目由多个AI工具协作开发：

| AI工具 | 负责模块 |
|--------|---------|
| **Claude Code** | 后端业务服务、数据库设计 |
| **Codex** | 单元测试、代码优化、Bug修复 |
| **Cursor** | 前端页面开发、组件封装 |
| **Trae** | 全栈集成、系统联调 |
| **扣子** | AI大模型接口对接、智能算法 |
| **WorkBuddy** | 自动化运维、部署监控 |

详细角色说明请参阅 [AGENTS.md](AGENTS.md)

---

## 📋 开发流程

1. **阅读文档**：阅读《AI快捷执行手册》了解任务
2. **检查状态**：确认任务状态和依赖
3. **执行任务**：按照任务卡片要求开发
4. **更新状态**：完成后更新任务状态和执行日志
5. **审计验收**：等待审计通过

---

## 🤝 贡献指南

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 📝 变更日志

请参阅 [CHANGELOG.md](CHANGELOG.md)

---

## 📄 许可证

本项目仅供内部开发使用。

---

## 📞 联系方式

如有问题，请联系项目负责人。