# AI 大模型配置指南

## 快速开始

本项目支持通过 `application.yml` 或环境变量配置 AI 大模型。系统采用统一的 `DefaultLlmClient` 调度所有 AI 能力，
包括：题目生成、学情分析、激励文案、错因分析等。

## 配置方式

### 方式一：`application.yml`（开发环境）

```yaml
ai:
  llm:
    provider: deepseek          # 服务商标识
    base-url: https://api.deepseek.com/v1/chat/completions
    api-key: sk-你的API密钥
    model: deepseek-chat
    timeout-ms: 8000
    max-retries: 2
    cache-enabled: true
    cache-ttl-seconds: 1800
```

### 方式二：环境变量（生产环境）

在 `application-prod.yml` 中已配置为从环境变量读取：

| 环境变量 | 说明 | 示例 |
|---|---|---|
| `AI_LLM_PROVIDER` | 服务商 | `deepseek` |
| `AI_LLM_BASE_URL` | API 地址 | `https://api.deepseek.com/v1/chat/completions` |
| `AI_LLM_API_KEY` | API 密钥 | `sk-...` |
| `AI_LLM_MODEL` | 模型名 | `deepseek-chat` |
| `AI_LLM_TIMEOUT_MS` | 超时（毫秒） | `8000` |
| `AI_LLM_MAX_RETRIES` | 最大重试次数 | `2` |

## 支持的 Provider

API 格式兼容 **OpenAI Chat Completions** 的所有服务均可使用：

### DeepSeek（推荐）
- **价格**：约 ¥1/百万 token（输入），¥2/百万 token（输出）
- **配置**：
  ```yaml
  provider: deepseek
  base-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
  ```

### OpenAI
- **配置**：
  ```yaml
  provider: openai
  base-url: https://api.openai.com/v1/chat/completions
  model: gpt-4o-mini
  ```

### Ollama（本地部署）
- **配置**：
  ```yaml
  provider: ollama
  base-url: http://localhost:11434/v1/chat/completions
  api-key: ollama
  model: qwen2.5:7b
  ```

## 缓存策略

- 默认启用 Redis 缓存（TTL: 30分钟）
- 相同 `taskType + userPrompt` 的查询命中缓存后直接返回
- 可通过 `ai.llm.cache-enabled: false` 关闭

## 降级策略

- 未配置远程 LLM 时，系统使用 `local` 模式
- local 模式下各服务使用确定性规则生成内容（非 AI 生成，用于开发和演示）
- 生产环境（`spring.profiles.active=prod`）未配置远程 LLM 时，相关接口返回 503 明确报错

## 验证是否生效

启动后端后访问管理后台 `/api/v1/admin/monitor`，查看 `aiStatus` 字段：

```json
{
  "aiStatus": {
    "modelName": "deepseek-chat",
    "provider": "deepseek",
    "apiStatus": "已配置"
  }
}
```

如果 `apiStatus` 显示 "未配置"，则系统运行在 local 降级模式。

## 使用场景一览

| 场景 | taskType | 默认 fallback |
|------|----------|---------------|
| 题目生成 | `question_generation` | 本地固定题库 + 模板题 |
| 学情分析 | `learning_analysis` | 按错题数量计算权重 |
| 激励文案 | `incentive` | 预设模板文案 |
| 错因分析 | `error_analysis` | 规则化错因归类 |
| 月度报告 | `report` | 模板化总结文案 |
