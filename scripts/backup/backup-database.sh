#!/bin/bash

# Database backup script for LabelReader
# This script creates automated MySQL backups with rotation

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups/mysql}"
CONTAINER_NAME="${CONTAINER_NAME:-labelreader-db-prod}"
DB_NAME="${DB_NAME:-labelreader}"
DB_USER="${DB_USER:-labelreader_user}"
DB_PASSWORD="${DB_PASSWORD}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/labelreader_${TIMESTAMP}.sql.gz"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Function to log messages
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Function to handle errors
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check if container is running
if ! docker ps | grep -q "${CONTAINER_NAME}"; then
    error_exit "Container ${CONTAINER_NAME} is not running"
fi

log "Starting database backup..."

# Create backup
docker exec "${CONTAINER_NAME}" mysqldump \
    -u"${DB_USER}" \
    -p"${DB_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    "${DB_NAME}" | gzip > "${BACKUP_FILE}" || error_exit "Backup failed"

# Check if backup was created successfully
if [ ! -f "${BACKUP_FILE}" ]; then
    error_exit "Backup file was not created"
fi

# Get backup size
BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
log "Backup created successfully: ${BACKUP_FILE} (${BACKUP_SIZE})"

# Verify backup integrity
if gunzip -t "${BACKUP_FILE}" 2>/dev/null; then
    log "Backup integrity verified"
else
    error_exit "Backup integrity check failed"
fi

# Cleanup old backups
log "Cleaning up backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "labelreader_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -delete

# Count remaining backups
BACKUP_COUNT=$(find "${BACKUP_DIR}" -name "labelreader_*.sql.gz" -type f | wc -l)
log "Total backups retained: ${BACKUP_COUNT}"

log "Backup completed successfully"

exit 0
