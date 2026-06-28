#!/usr/bin/env bash
# ============================================================
# SuperStudy MySQL 自动备份脚本
# 由 ScheduledTasks.recordHeartbeat() 每日调用
# ============================================================
# 配置：通过环境变量 DB_HOST / DB_USER / DB_PASS / DB_NAME
# 默认值：localhost / root / root / superstudy
# ============================================================

set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="${DB_NAME:-superstudy}"
BACKUP_DIR="${BACKUP_DIR:-/app/backups}"
DATE_TAG=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE_TAG}.sql.gz"

mkdir -p "$BACKUP_DIR"

if command -v mysqldump &>/dev/null; then
    MYSQL_PWD="$DB_PASS" mysqldump -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" 2>/dev/null | gzip > "$BACKUP_FILE"
    echo "BACKUP_OK:$BACKUP_FILE"
else
    echo "SKIPPED:mysqldump not available"
    exit 0
fi
