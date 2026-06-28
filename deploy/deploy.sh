#!/usr/bin/env bash
# ============================================================
# SuperStudy 全栈一键部署脚本
# ============================================================
# 用法：
#   bash deploy/deploy.sh                 交互式完整部署
#   bash deploy/deploy.sh --check         检查部署就绪度
#   bash deploy/deploy.sh --docker        构建并启动 Docker
#   bash deploy/deploy.sh --zeabur        推送数据库到 Zeabur
#   bash deploy/deploy.sh --frontend      部署前端到 Vercel
# ============================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  🚀 SuperStudy 全栈部署工具                 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ============================================================
# 检查模式
# ============================================================
check_mode() {
    local issues=0

    echo -e "${YELLOW}===== 部署就绪度检查 =====${NC}"
    echo ""

    # 1. JDK
    JAVA_VER=""
    if command -v java &>/dev/null && java -version &>/dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*version "\(.*\)" .*/\1/')
    fi
    if [[ -n "$JAVA_VER" ]]; then
        echo -e "  JDK:       ${GREEN}$JAVA_VER${NC}"
    else
        echo -e "  JDK:       ${YELLOW}缺失（设置 JAVA_HOME）${NC}"
    fi

    # 2. Maven
    if command -v mvn &>/dev/null; then
        MVN_VER=$(mvn --version 2>&1 | head -1 | sed 's/.*Apache Maven \([0-9.]*\).*/\1/') || MVN_VER=""
        echo -e "  Maven:     ${GREEN}${MVN_VER:-已安装}${NC}"
    else
        echo -e "  Maven:     ${RED}缺失${NC}"
        ((issues++))
    fi

    # 3. Docker
    if command -v docker &>/dev/null; then
        DOCKER_VER=$(docker --version 2>&1)
        echo -e "  Docker:    ${GREEN}$DOCKER_VER${NC}"
    else
        echo -e "  Docker:    ${YELLOW}缺失（可选）${NC}"
    fi

    # 4. Redis
    if command -v redis-cli &>/dev/null && redis-cli ping &>/dev/null; then
        echo -e "  Redis:     ${GREEN}运行中${NC}"
    else
        echo -e "  Redis:     ${YELLOW}未运行（集成测试需要）${NC}"
    fi

    # 5. .env
    if [[ -f "$PROJECT_ROOT/.env" ]]; then
        echo -e "  .env:      ${GREEN}已存在${NC}"
    else
        echo -e "  .env:      ${YELLOW}未配置${NC}"
    fi

    # 6. JWT_SECRET
    JWT_SECRET="${JWT_SECRET:-}"
    if [[ -z "$JWT_SECRET" ]] && [[ -f "$PROJECT_ROOT/.env" ]] && grep -q "JWT_SECRET" "$PROJECT_ROOT/.env" 2>/dev/null; then
        echo -e "  JWT_SECRET:${GREEN}已配置${NC}"
    elif [[ -z "$JWT_SECRET" ]]; then
        echo -e "  JWT_SECRET:${RED}未配置${NC}"
        ((issues++))
    else
        echo -e "  JWT_SECRET:${GREEN}已配置（环境变量）${NC}"
    fi

    # 7. AI LLM
    LLM_KEY="${AI_LLM_API_KEY:-}"
    if [[ -z "$LLM_KEY" ]] && [[ -f "$PROJECT_ROOT/.env" ]]; then
        LLM_KEY=$(grep "^AI_LLM_API_KEY" "$PROJECT_ROOT/.env" 2>/dev/null | cut -d= -f2) || true
    fi
    if [[ -n "$LLM_KEY" ]]; then
        echo -e "  AI LLM:    ${GREEN}已配置${NC}"
    else
        echo -e "  AI LLM:    ${YELLOW}未配置（local降级）${NC}"
    fi

    # 8. 后端编译
    if [[ -f "$PROJECT_ROOT/backend/pom.xml" ]]; then
        echo -e "  后端项目: ${GREEN}存在${NC}"
    fi

    # 9. 前端构建
    if [[ -d "$PROJECT_ROOT/frontend/dist" ]]; then
        echo -e "  前端构建: ${GREEN}存在${NC}"
    fi

    echo ""
    if [[ $issues -eq 0 ]]; then
        echo -e "${GREEN}✅ 部署就绪（$issues 个阻塞）${NC}"
    else
        echo -e "${RED}❌ 存在 $issues 个阻塞问题${NC}"
    fi
}

# ============================================================
# Docker 构建并启动
# ============================================================
docker_deploy() {
    echo -e "${YELLOW}===== Docker 构建并启动 =====${NC}"
    echo ""

    # 检查 .env
    if [[ ! -f "$PROJECT_ROOT/.env" ]]; then
        echo -e "${YELLOW}⚠️  未找到 .env 文件，创建模板...${NC}"
        cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
        echo -e "${RED}请先编辑 .env 填入真实值，然后重新运行${NC}"
        echo "  编辑: nano $PROJECT_ROOT/.env"
        exit 1
    fi

    # 确保 JWT_SECRET 存在
    if ! grep -q "JWT_SECRET=" "$PROJECT_ROOT/.env" 2>/dev/null || grep -q "your-random" "$PROJECT_ROOT/.env" 2>/dev/null; then
        echo -e "${YELLOW}⚠️  生成 JWT_SECRET...${NC}"
        NEW_SECRET=$(openssl rand -hex 32 2>/dev/null || date +%s | md5 | head -32)
        if [[ "$(uname)" == "Darwin" ]]; then
            sed -i '' "s/JWT_SECRET=.*/JWT_SECRET=$NEW_SECRET/" "$PROJECT_ROOT/.env"
        else
            sed -i "s/JWT_SECRET=.*/JWT_SECRET=$NEW_SECRET/" "$PROJECT_ROOT/.env"
        fi
        echo -e "${GREEN}  JWT_SECRET 已生成${NC}"
    fi

    echo -e "${CYAN}构建镜像...${NC}"
    docker compose -f "$PROJECT_ROOT/docker-compose.yml" build

    echo -e "${CYAN}启动服务...${NC}"
    docker compose -f "$PROJECT_ROOT/docker-compose.yml" up -d

    echo ""
    echo -e "${GREEN}✅ 服务已启动${NC}"
    echo -e "  前端: http://localhost"
    echo -e "  后端: http://localhost:8080"
    echo -e ""
    echo -e "查看日志: docker compose logs -f"
    echo -e "停止服务: docker compose down"
}

# ============================================================
# 前端 Vercel 部署
# ============================================================
vercel_deploy() {
    echo -e "${YELLOW}===== 部署前端到 Vercel =====${NC}"
    echo ""

    if ! command -v vercel &>/dev/null; then
        echo -e "${RED}❌ vercel CLI 未安装${NC}"
        echo -e "安装: npm install -g vercel"
        exit 1
    fi

    cd "$PROJECT_ROOT/frontend"

    # 确保生产环境 API 地址
    echo -e "${CYAN}当前 API 代理目标:${NC}"
    grep "destination" vercel.json 2>/dev/null || echo "  （使用相对路径）"

    echo ""
    echo -e "${CYAN}部署到 Vercel...${NC}"
    vercel --prod

    echo ""
    echo -e "${GREEN}✅ 前端已部署${NC}"
}

# ============================================================
# Zeabur 数据库初始化
# ============================================================
zeabur_init_db() {
    echo -e "${YELLOW}===== 初始化 Zeabur MySQL =====${NC}"
    echo ""
    echo -e "请将以下 SQL 粘贴到 Zeabur MySQL 控制台执行："
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "文件: deploy/zeabur-schema.sql"
    echo -e "大小: $(wc -c < "$PROJECT_ROOT/deploy/zeabur-schema.sql") 字节"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "或使用 MySQL CLI："
    echo -e "  mysql -h <host> -u <user> -p < deploy/zeabur-schema.sql"
    echo ""
}

# ============================================================
# 主入口
# ============================================================
case "${1:-}" in
    --check|-c)
        check_mode
        ;;
    --docker|-d)
        docker_deploy
        ;;
    --frontend|-f)
        vercel_deploy
        ;;
    --zeabur|-z)
        zeabur_init_db
        ;;
    *)
        echo -e "用法: bash deploy/deploy.sh [选项]"
        echo ""
        echo -e "   ${CYAN}--check${NC}     检查部署就绪度"
        echo -e "   ${CYAN}--docker${NC}    构建并启动 Docker 本地生产环境"
        echo -e "   ${CYAN}--frontend${NC}  部署前端到 Vercel"
        echo -e "   ${CYAN}--zeabur${NC}    初始化 Zeabur 数据库"
        echo ""
        echo -e "无参数：交互式全流程部署"
        echo ""
        echo -e "${YELLOW}===== 交互式部署 =====${NC}"
        echo ""
        check_mode
        echo ""
        echo -e "请选择操作："
        echo -e "  ${CYAN}1)${NC} Docker 构建并本地启动"
        echo -e "  ${CYAN}2)${NC} 部署前端到 Vercel"
        echo -e "  ${CYAN}3)${NC} 初始化 Zeabur 数据库"
        echo -e "  ${CYAN}q)${NC} 退出"
        echo ""
        read -rp "请输入选项 [1]: " choice
        case "${choice:-1}" in
            1) docker_deploy ;;
            2) vercel_deploy ;;
            3) zeabur_init_db ;;
            q|Q) exit 0 ;;
            *) echo -e "${RED}无效选项${NC}" ;;
        esac
        ;;
esac
