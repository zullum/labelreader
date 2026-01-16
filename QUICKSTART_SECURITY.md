# Security & Monitoring Quick Start Guide

## Prerequisites
- Docker and Docker Compose installed
- Maven 3.9+ (for building backend)
- Access to production server

## Step 1: Configure Environment Variables

```bash
# Copy example file
cp .env.production.example .env.production

# Edit with your values
nano .env.production
```

**Critical values to change**:
- `DB_ROOT_PASSWORD`: Strong database password
- `DB_PASSWORD`: Application database password
- `JWT_SECRET`: At least 256-bit secret key
- `CORS_ALLOWED_ORIGINS`: Your production domains
- `GRAFANA_ADMIN_PASSWORD`: Grafana admin password
- `SMTP_*`: Email configuration for alerts
- `ALERT_EMAIL_*`: Alert recipient emails

## Step 2: Build Application with Security Features

```bash
# Navigate to backend
cd apps/backend

# Build with Maven
mvn clean package -DskipTests

# Return to root
cd ../..
```

## Step 3: Deploy Application

```bash
# Start production stack
docker-compose -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f backend
```

## Step 4: Deploy Monitoring Stack

```bash
# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Check status
docker-compose -f docker-compose.monitoring.yml ps

# View Grafana logs
docker-compose -f docker-compose.monitoring.yml logs -f grafana
```

## Step 5: Configure Database Backups

```bash
# Make scripts executable
chmod +x scripts/backup/*.sh

# Test backup manually
export DB_PASSWORD="your-db-password"
export BACKUP_DIR="/backups/mysql"
./scripts/backup/backup-database.sh

# Set up automated backups (cron)
# Edit crontab file with your paths
nano scripts/backup/crontab-backup

# Install crontab
crontab scripts/backup/crontab-backup

# Verify
crontab -l
```

## Step 6: Verify Security Features

### 1. Check CORS Configuration
```bash
curl -H "Origin: https://yourdomain.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Authorization" \
     -X OPTIONS \
     http://localhost:8080/api/public/test
```

Expected: Should see CORS headers in response

### 2. Test Rate Limiting
```bash
# Send multiple requests quickly
for i in {1..15}; do
  curl http://localhost:8080/api/auth/login \
       -H "Content-Type: application/json" \
       -d '{"email":"test@test.com","password":"test"}' \
       -w "\n%{http_code}\n"
done
```

Expected: After 10 requests, should receive HTTP 429

### 3. Check Security Headers
```bash
curl -I http://localhost:8080/api/public/test
```

Expected headers:
- `X-XSS-Protection: 1; mode=block`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Content-Security-Policy: default-src 'self'...`

### 4. Verify Metrics Endpoint
```bash
curl http://localhost:8080/actuator/prometheus
```

Expected: Prometheus metrics in text format

## Step 7: Access Monitoring Dashboards

### Grafana
- URL: http://localhost:3000
- Username: `admin` (from GRAFANA_ADMIN_USER)
- Password: (from GRAFANA_ADMIN_PASSWORD)

**First Login**:
1. Change admin password
2. Navigate to "Dashboards"
3. Open "LabelReader Application Overview"

### Prometheus
- URL: http://localhost:9090
- No authentication required (internal only)

**Test Queries**:
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Memory usage
jvm_memory_used_bytes{area="heap"}
```

### Alertmanager
- URL: http://localhost:9093
- No authentication required (internal only)

## Step 8: Test Backup & Restore

### Create Test Backup
```bash
./scripts/backup/backup-database.sh
```

Expected output:
```
[2024-01-01 02:00:00] Starting database backup...
[2024-01-01 02:00:05] Backup created successfully: /backups/mysql/labelreader_20240101_020000.sql.gz (15M)
[2024-01-01 02:00:05] Backup integrity verified
```

### List Backups
```bash
ls -lh /backups/mysql/
```

### Test Restore (Optional)
```bash
# WARNING: This will replace database content
./scripts/backup/restore-database.sh /backups/mysql/labelreader_YYYYMMDD_HHMMSS.sql.gz
```

## Troubleshooting

### Backend Fails to Start

**Check logs**:
```bash
docker-compose -f docker-compose.prod.yml logs backend
```

**Common issues**:
- Database connection: Check `DB_*` environment variables
- JWT secret: Ensure `JWT_SECRET` is set and valid
- CORS: Check `CORS_ALLOWED_ORIGINS` format

### Monitoring Not Working

**Check Prometheus targets**:
1. Visit: http://localhost:9090/targets
2. Verify all targets are "UP"

**If backend metrics are down**:
```bash
# Test metrics endpoint directly
curl http://localhost:8080/actuator/prometheus

# Check backend logs
docker-compose -f docker-compose.prod.yml logs backend | grep -i actuator
```

### Rate Limiting Not Working

**Check interceptor registration**:
```bash
# Look for rate limit logs in backend
docker-compose logs backend | grep -i "rate limit"
```

**Test with verbose curl**:
```bash
curl -v http://localhost:8080/api/auth/login
```

Should see `X-Rate-Limit-Retry-After-Seconds` header when limited.

### Backup Fails

**Check permissions**:
```bash
# Create backup directory
sudo mkdir -p /backups/mysql
sudo chown $(whoami):$(whoami) /backups/mysql

# Check Docker access
docker exec labelreader-db-prod mysqladmin ping
```

## Security Checklist

Before going to production:

- [ ] Changed all default passwords in `.env.production`
- [ ] Generated strong JWT secret (256+ bits)
- [ ] Configured CORS for production domains only
- [ ] Set up SSL/TLS certificates
- [ ] Configured email alerts
- [ ] Tested backup and restore process
- [ ] Verified rate limiting works
- [ ] Checked all security headers
- [ ] Changed Grafana admin password
- [ ] Restricted Prometheus/Alertmanager access (firewall)
- [ ] Set up log rotation
- [ ] Configured automated backups (cron)
- [ ] Tested monitoring alerts
- [ ] Reviewed and adjusted rate limits for your use case

## Maintenance Commands

### View All Logs
```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend

# Monitoring logs
docker-compose -f docker-compose.monitoring.yml logs -f prometheus
```

### Restart Services
```bash
# Restart backend
docker-compose -f docker-compose.prod.yml restart backend

# Restart all
docker-compose -f docker-compose.prod.yml restart

# Restart monitoring
docker-compose -f docker-compose.monitoring.yml restart
```

### Update Application
```bash
# Pull latest code
git pull

# Rebuild
cd apps/backend && mvn clean package && cd ../..

# Recreate containers
docker-compose -f docker-compose.prod.yml up -d --build
```

### Manual Backup
```bash
# One-time backup
./scripts/backup/backup-database.sh

# Backup with custom retention
RETENTION_DAYS=60 ./scripts/backup/backup-database.sh
```

### Check Disk Space
```bash
# System disk usage
df -h

# Docker volumes
docker system df

# Backup directory
du -sh /backups/mysql/
```

## Next Steps

1. **Configure SSL/TLS**: Set up HTTPS with Let's Encrypt or your certificate
2. **Set up Log Aggregation**: Configure ELK stack or CloudWatch
3. **Configure CDN**: Use CloudFront, Cloudflare, or similar for static assets
4. **Enable Database Encryption**: Configure MySQL encryption at rest
5. **Set up WAF**: Web Application Firewall for additional protection
6. **Implement 2FA**: Add two-factor authentication for admin accounts
7. **Security Audit**: Run OWASP ZAP or similar security scanner
8. **Load Testing**: Test rate limits and performance under load

## Support & Documentation

- Full Security Guide: `SECURITY_SETUP_GUIDE.md`
- Security Audit: `SECURITY_AUDIT.md`
- Deployment Guide: `DEPLOYMENT_QUICKSTART.md`
- API Reference: `API_REFERENCE_PHASE4.md`

## Emergency Contacts

In case of security incident:
1. Stop affected services: `docker-compose -f docker-compose.prod.yml stop`
2. Review logs for indicators of compromise
3. Check Alertmanager for security alerts
4. Restore from last known good backup if needed
5. Contact security team (configure in alertmanager.yml)
