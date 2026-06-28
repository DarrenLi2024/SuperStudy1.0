#!/usr/bin/env bash
# ============================================================
# SuperStudy AI 后端 — 一键启动脚本（DeepSeek 模式）
# ============================================================
# 用法：bash scripts/start-with-llm.sh
# ============================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  🤖 SuperStudy AI 大模型 — 一键启动        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "即将启动后端，AI 配置："
echo -e "  ${CYAN}Provider:${NC} deepseek"
echo -e "  ${CYAN}Model:${NC}    deepseek-chat"
echo -e "  ${CYAN}API URL:${NC}  https://api.deepseek.com/v1/chat/completions"
echo ""

# 检查是否已有 API 密钥
if [[ -z "${AI_LLM_API_KEY:-}" ]]; then
    echo -e "${YELLOW}⚠️  未检测到 AI_LLM_API_KEY 环境变量${NC}"
    echo -e "请粘贴你的 DeepSeek API 密钥（粘贴后按回车）："
    echo -e "  ${YELLOW}（如果没有密钥，去 https://platform.deepseek.com 注册）${NC}"
    echo ""
    read -r -p "API Key: " API_KEY
    if [[ -z "$API_KEY" ]]; then
        echo -e "${RED}密钥为空，将以 local 降级模式启动${NC}"
        echo "export AI_LLM_PROVIDER=local" > /dev/null
    else
        export AI_LLM_API_KEY="$API_KEY"
    fi
fi

# 设置 JDK 17
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export AI_LLM_PROVIDER="${AI_LLM_PROVIDER:-deepseek}"
export AI_LLM_BASE_URL="${AI_LLM_BASE_URL:-https://api.deepseek.com/v1/chat/completions}"
export AI_LLM_MODEL="${AI_LLM_MODEL:-deepseek-chat}"

# 屏蔽密钥的完整输出
KEY_MASK=""
if [[ -n "${AI_LLM_API_KEY:-}" ]]; then
    KEY_LEN=${#AI_LLM_API_KEY}
    if [[ $KEY_LEN -gt 12 ]]; then
        KEY_MASK="${AI_LLM_API_KEY:0:8}****${AI_LLM_API_KEY: -4}"
    else
        KEY_MASK="****"
    fi
fi

echo ""
echo -e "${GREEN}┌─────────────────────────────────────────────┐${NC}"
echo -e "${GREEN}│ 🤖 AI 配置                                  │${NC}"
echo -e "${GREEN}│     Provider : $AI_LLM_PROVIDER${NC}"
echo -e "${GREEN}│     Model    : $AI_LLM_MODEL${NC}"
echo -e "${GREEN}│     API Key  : ${KEY_MASK:-未设置}${NC}"
echo -e "${GREEN}└─────────────────────────────────────────────┘${NC}"
echo ""
echo -e "${YELLOW}按 Ctrl+C 可随时停止服务器${NC}"
echo -e "启动中..."
echo ""

cd "$PROJECT_ROOT/backend"
# 只在密钥非空时设置 API_KEY 环境变量
if [[ -n "${AI_LLM_API_KEY:-}" ]]; then
    exec env JAVA_HOME="$JAVA_HOME" \
         AI_LLM_PROVIDER="$AI_LLM_PROVIDER" \
         AI_LLM_BASE_URL="$AI_LLM_BASE_URL" \
         AI_LLM_API_KEY="$AI_LLM_API_KEY" \
         AI_LLM_MODEL="$AI_LLM_MODEL" \
         mvn spring-boot:run
else
    exec mvn spring-boot:run
fi
