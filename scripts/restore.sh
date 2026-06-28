#!/bin/bash
# ============================================
# 高中全科AI升学成长陪伴系统 - 数据库恢复脚本
# 使用方法: ./restore.sh <备份文件>
# 示例: ./restore.sh /backup/superstudy_20260628_020000.sql.gz
# ============================================

set -e

# 配置
DB_NAME="superstudy"
DB_USER="root"
DB_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"

# 检查参数
if [ $# -lt 1 ]; then
    echo "错误: 请指定备份文件"
    echo "使用方法: $0 <备份文件路径>"
    echo "示例: $0 /backup/superstudy_20260628_020000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

# 检查文件是否存在
if [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误: 备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

echo "============================================"
echo "数据库恢复脚本"
echo "============================================"
echo "数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "备份文件: ${BACKUP_FILE}"
echo ""

# 确认恢复
read -p "警告: 恢复操作将覆盖当前数据库。是否继续? (y/N): " CONFIRM
if [ "${CONFIRM}" != "y" ] && [ "${CONFIRM}" != "Y" ]; then
    echo "已取消恢复操作"
    exit 0
fi

echo "正在恢复数据库..."

# 执行恢复
if [[ "${BACKUP_FILE}" == *.gz ]]; then
    gunzip -c "${BACKUP_FILE}" | mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}"
else
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" < "${BACKUP_FILE}"
fi

if [ $? -eq 0 ]; then
    echo "恢复成功!"
    echo "备份文件: ${BACKUP_FILE}"
else
    echo "错误: 恢复失败!"
    exit 1
fi

echo "============================================"
