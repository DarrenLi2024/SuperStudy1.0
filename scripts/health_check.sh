#!/bin/bash
# ============================================
# 高中全科AI升学成长陪伴系统 - 健康检查脚本
# 检查各服务运行状态和关键接口
# ============================================

set -e

# 配置
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="superstudy"
DB_USER="root"
DB_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

check() {
    local name="$1"
    local status="$2"
    local message="$3"

    if [ "${status}" -eq 0 ]; then
        echo -e "  ${GREEN}[PASS]${NC} ${name}"
        PASS=$((PASS + 1))
    elif [ "${status}" -eq 2 ]; then
        echo -e "  ${YELLOW}[WARN]${NC} ${name} - ${message}"
        WARN=$((WARN + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} ${name} - ${message}"
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo "============================================"
echo "  SuperStudy 系统健康检查"
echo "============================================"
echo ""

# 1. MySQL 检查
echo "[数据库检查]"
if command -v mysqladmin &> /dev/null; then
    mysqladmin ping -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" --silent 2>/dev/null
    check "MySQL 服务" $? ""
else
    check "MySQL 服务" 2 "mysqladmin 命令不可用"
fi

# 2. Redis 检查
echo "[Redis检查]"
if command -v redis-cli &> /dev/null; then
    redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ping 2>/dev/null | grep -q "PONG"
    check "Redis 服务" $? ""
else
    check "Redis 服务" 2 "redis-cli 命令不可用"
fi

# 3. 后端 API 检查
echo "[后端检查]"
if command -v curl &> /dev/null; then
    # 健康检查接口
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/api/v1/user/login" -X POST -H "Content-Type: application/json" -d '{"username":"test","password":"test"}' 2>/dev/null || echo "000")

    if [ "${HTTP_CODE}" = "000" ]; then
        check "后端服务" 1 "无法连接 (${BACKEND_URL})"
    elif [ "${HTTP_CODE}" = "400" ] || [ "${HTTP_CODE}" = "401" ]; then
        check "后端服务" 0 ""
    else
        check "后端服务" 0 ""
    fi
else
    check "后端服务" 2 "curl 命令不可用"
fi

# 4. 前端检查
echo "[前端检查]"
if command -v curl &> /dev/null; then
    FRONTEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${FRONTEND_URL}" 2>/dev/null || echo "000")
    if [ "${FRONTEND_CODE}" = "000" ]; then
        check "前端服务" 1 "无法连接 (${FRONTEND_URL})"
    elif [ "${FRONTEND_CODE}" -ge 200 ] && [ "${FRONTEND_CODE}" -lt 400 ]; then
        check "前端服务" 0 ""
    else
        check "前端服务" 1 "HTTP ${FRONTEND_CODE}"
    fi
else
    check "前端服务" 2 "curl 命令不可用"
fi

# 5. Docker 检查
echo "[Docker检查]"
if command -v docker &> /dev/null; then
    DOCKER_OK=$(docker info --format '{{.ServerVersion}}' 2>/dev/null || echo "")
    if [ -n "${DOCKER_OK}" ]; then
        check "Docker 引擎" 0 ""

        # 检查容器运行状态
        for CONTAINER in superstudy-mysql superstudy-redis superstudy-backend superstudy-frontend; do
            STATUS=$(docker inspect --format='{{.State.Status}}' "${CONTAINER}" 2>/dev/null || echo "not-found")
            if [ "${STATUS}" = "running" ]; then
                check "  容器 ${CONTAINER}" 0 ""
            elif [ "${STATUS}" = "not-found" ]; then
                check "  容器 ${CONTAINER}" 2 "未运行"
            else
                check "  容器 ${CONTAINER}" 1 "状态: ${STATUS}"
            fi
        done
    else
        check "Docker 引擎" 1 "Docker 未运行"
    fi
else
    check "Docker 引擎" 2 "docker 命令不可用"
fi

# 6. 磁盘检查
echo "[磁盘检查]"
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "${DISK_USAGE}" -lt 80 ]; then
    check "磁盘使用率 (${DISK_USAGE}%)" 0 ""
elif [ "${DISK_USAGE}" -lt 90 ]; then
    check "磁盘使用率 (${DISK_USAGE}%)" 2 "超过 80%"
else
    check "磁盘使用率 (${DISK_USAGE}%)" 1 "超过 90%"
fi

# 7. 内存检查
echo "[内存检查]"
if command -v free &> /dev/null; then
    MEM_TOTAL=$(free -m | awk '/^Mem:/{print $2}')
    MEM_USED=$(free -m | awk '/^Mem:/{print $3}')
    MEM_PERCENT=$((MEM_USED * 100 / MEM_TOTAL))
    if [ "${MEM_PERCENT}" -lt 80 ]; then
        check "内存使用率 (${MEM_PERCENT}%)" 0 ""
    elif [ "${MEM_PERCENT}" -lt 90 ]; then
        check "内存使用率 (${MEM_PERCENT}%)" 2 "超过 80%"
    else
        check "内存使用率 (${MEM_PERCENT}%)" 1 "超过 90%"
    fi
else
    check "内存检查" 2 "free 命令不可用"
fi

# 总结
echo ""
echo "============================================"
echo -e "  检查结果: ${GREEN}${PASS} 通过${NC}, ${RED}${FAIL} 失败${NC}, ${YELLOW}${WARN} 警告${NC}"
echo "============================================"
echo ""

# 如果有失败项，退出码为非0
if [ "${FAIL}" -gt 0 ]; then
    exit 1
fi
exit 0
