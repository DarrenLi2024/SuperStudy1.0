#!/bin/bash
# ============================================
# 高中全科AI升学成长陪伴系统 - 数据库备份脚本
# 每日全量备份，保留30天
# ============================================

set -e

# 配置
BACKUP_DIR="/backup"
DB_NAME="superstudy"
DB_USER="root"
DB_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
RETENTION_DAYS=30
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"
LOG_FILE="${BACKUP_DIR}/backup.log"

# 确保备份目录存在
mkdir -p "${BACKUP_DIR}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${LOG_FILE}"
}

log "========== 开始数据库备份 =========="
log "数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"

# 执行备份
log "正在执行 mysqldump..."
mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --databases "${DB_NAME}" \
    | gzip > "${BACKUP_FILE}"

# 检查备份是否成功
if [ $? -eq 0 ] && [ -f "${BACKUP_FILE}" ]; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    log "备份成功: ${BACKUP_FILE} (${BACKUP_SIZE})"
else
    log "错误: 备份失败!"
    exit 1
fi

# 删除过期备份
log "清理 ${RETENTION_DAYS} 天前的备份..."
find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -mtime +${RETENTION_DAYS} -delete

# 统计
BACKUP_COUNT=$(find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" | wc -l)
BACKUP_TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" | cut -f1)
log "当前备份数量: ${BACKUP_COUNT}, 总大小: ${BACKUP_TOTAL_SIZE}"
log "========== 备份完成 =========="
echo ""
