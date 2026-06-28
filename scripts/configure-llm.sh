#!/usr/bin/env bash
# ============================================================
# AI 大模型一键配置脚本
# ============================================================
# 用法：
#   bash scripts/configure-llm.sh            # 交互式配置
#   bash scripts/configure-llm.sh --check     # 仅检查当前配置
# ============================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"

echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   🤖 SuperStudy AI 大模型配置工具           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ---------- 检查模式 ----------
if [[ "${1:-}" == "--check" ]]; then
    echo -e "${YELLOW}检查当前 AI 配置状态...${NC}"
    echo ""

    if [[ -f "$BACKEND_DIR/src/main/resources/application-dev.yml" ]]; then
        PROVIDER=$(grep -E "^[[:space:]]*provider:" "$BACKEND_DIR/src/main/resources/application-dev.yml" | head -1 | awk '{print $2}')
        echo -e "  application-dev.yml 存在：${GREEN}✅${NC}"
        echo -e "  Provider: ${CYAN}${PROVIDER:-未知}${NC}"
    else
        echo -e "  application-dev.yml：${YELLOW}不存在（使用 local 降级）${NC}"
    fi

    LLM_PROVIDER="${AI_LLM_PROVIDER:-}"
    LLM_API_KEY="${AI_LLM_API_KEY:-}"
    LLM_BASE_URL="${AI_LLM_BASE_URL:-}"
    if [[ -n "$LLM_PROVIDER" && -n "$LLM_API_KEY" && "$LLM_PROVIDER" != "local" ]]; then
        echo -e "  环境变量已配置：${GREEN}✅${NC}"
        echo -e "  Provider: ${CYAN}$LLM_PROVIDER${NC}"
        echo -e "  Base URL: $LLM_BASE_URL"
        KEY_MASK="${LLM_API_KEY:0:8}****${LLM_API_KEY: -4}"
        echo -e "  API Key: $KEY_MASK"
    else
        echo -e "  环境变量未配置：${YELLOW}⚠️${NC}"
    fi
    echo ""
    echo -e "当前模式：$([[ -n "$LLM_PROVIDER" && "$LLM_PROVIDER" != "local" ]] || [[ -f "$BACKEND_DIR/src/main/resources/application-dev.yml" ]] && echo "${GREEN}远程模式${NC}" || echo "${YELLOW}本地降级模式${NC}")"
    echo ""
    exit 0
fi

# ---------- 交互式配置 ----------
echo -e "选择 LLM Provider："
echo -e "  ${CYAN}1)${NC} DeepSeek  （推荐，¥1/百万token，国内直连）"
echo -e "  ${CYAN}2)${NC} OpenAI     （需海外网络）"
echo -e "  ${CYAN}3)${NC} Ollama     （本地部署，免费）"
echo -e "  ${CYAN}q)${NC} 取消"
echo ""
read -rp "请输入选项 [1]: " provider_choice
provider_choice="${provider_choice:-1}"

case "$provider_choice" in
    1|"")
        PROVIDER="deepseek"
        BASE_URL="https://api.deepseek.com/v1/chat/completions"
        MODEL="deepseek-chat"
        PROVIDER_NAME="DeepSeek"
        ;;
    2)
        PROVIDER="openai"
        BASE_URL="https://api.openai.com/v1/chat/completions"
        MODEL="gpt-4o-mini"
        PROVIDER_NAME="OpenAI"
        ;;
    3)
        PROVIDER="ollama"
        BASE_URL="http://localhost:11434/v1/chat/completions"
        MODEL="qwen2.5:7b"
        PROVIDER_NAME="Ollama"
        echo -e "${YELLOW}Ollama 需要在本地先启动服务：ollama serve${NC}"
        ;;
    q|Q)
        echo "已取消。"
        exit 0
        ;;
    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "请输入 ${CYAN}${PROVIDER_NAME}${NC} API 密钥："
read -rp "API Key: " API_KEY

if [[ -z "$API_KEY" ]]; then
    echo -e "${YELLOW}密钥为空，配置将被清空（下次启动仍为 local 降级模式）${NC}"
fi

CONFIG_DIR="$BACKEND_DIR/src/main/resources"
DEV_CONFIG="$CONFIG_DIR/application-dev.yml"

# 生成 application-dev.yml
cat > "$DEV_CONFIG" << END
# ========================
# AI 大模型配置 — 开发环境（由 configure-llm.sh 生成）
# 警告：此文件包含 API 密钥，已加入 .gitignore，不会提交到 Git
# ========================
ai:
  llm:
    provider: ${PROVIDER:-local}
    base-url: ${BASE_URL:-}
    api-key: ${API_KEY:-}
    model: ${MODEL:-local-deterministic}
    timeout-ms: 8000
    max-retries: 2
    cache-enabled: true
    cache-ttl-seconds: 1800
END

echo ""
echo -e "${GREEN}✅ 配置已写入：$DEV_CONFIG${NC}"
echo ""

# 验证
if [[ -n "$API_KEY" ]]; then
    echo -e "${GREEN}🎉 配置完成！${NC}"
    echo -e ""
    echo -e "启动方式："
    echo -e "  ${CYAN}SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run${NC}"
    echo -e ""
    echo -e "或设置环境变量（不依赖 dev profile）："
    echo -e "  ${CYAN}export AI_LLM_PROVIDER=$PROVIDER${NC}"
    echo -e "  ${CYAN}export AI_LLM_BASE_URL=$BASE_URL${NC}"
    echo -e "  ${CYAN}export AI_LLM_API_KEY='$API_KEY'${NC}"
    echo -e "  ${CYAN}export AI_LLM_MODEL=$MODEL${NC}"
    echo -e "  ${CYAN}mvn spring-boot:run${NC}"
    echo ""
    echo -e "启动后可在日志中看到 AI 配置状态："
    echo -e "  ${GREEN}🤖 AI 大模型模式：远程调用${NC}"
    echo -e "  Provider: $PROVIDER"
    echo -e "  Model: $MODEL"
else
    echo -e "${YELLOW}⚠️  密钥为空，系统仍将使用 local 降级模式${NC}"
    echo "重新运行本脚本填入密钥即可启用远程 AI。"
fi

echo ""
echo -e "${CYAN}━━━ 文档参考 ━━━${NC}"
echo -e "详细配置说明：docs/ai-config-guide.md"
echo -e "Provider 切换：docs/application-dev.example.yml"
