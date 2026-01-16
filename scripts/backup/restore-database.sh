#!/bin/bash

# Database restore script for LabelReader
# This script restores MySQL backups

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups/mysql}"
CONTAINER_NAME="${CONTAINER_NAME:-labelreader-db-prod}"
DB_NAME="${DB_NAME:-labelreader}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD}"

# Function to log messages
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Function to handle errors
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check if backup file is provided
if [ -z "$1" ]; then
    log "Usage: $0 <backup_file>"
    log "Available backups:"
    ls -lh "${BACKUP_DIR}"/labelreader_*.sql.gz 2>/dev/null || echo "No backups found"
    exit 1
fi

BACKUP_FILE="$1"

# Check if backup file exists
if [ ! -f "${BACKUP_FILE}" ]; then
    error_exit "Backup file not found: ${BACKUP_FILE}"
fi

# Check if container is running
if ! docker ps | grep -q "${CONTAINER_NAME}"; then
    error_exit "Container ${CONTAINER_NAME} is not running"
fi

log "Starting database restore from: ${BACKUP_FILE}"

# Confirm restore operation
read -p "WARNING: This will replace all data in database '${DB_NAME}'. Continue? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    log "Restore cancelled"
    exit 0
fi

# Restore backup
log "Restoring database..."
gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" mysql \
    -u"${DB_USER}" \
    -p"${DB_PASSWORD}" \
    "${DB_NAME}" || error_exit "Restore failed"

log "Database restored successfully from ${BACKUP_FILE}"

exit 0
