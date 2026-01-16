# Security Setup & Monitoring Guide

## Overview
This guide covers the implementation of critical security features and production monitoring for the LabelReader platform.

## Security Features Implemented

### 1. CORS Configuration
**Status**: ✅ Implemented

**Location**: `SecurityConfig.java:82-95`

**Configuration**:
- Configurable allowed origins via `app.cors.allowed-origins` property
- Restricted methods: GET, POST, PUT, DELETE, OPTIONS
- Specific allowed headers (Authorization, Content-Type, Accept)
- Credentials support enabled
- Max age: 3600 seconds (1 hour)

**Environment Configuration**:
```properties
# Development
app.cors.allowed-origins=http://localhost:4200,http://localhost:80

# Production
app.cors.allowed-origins=https://yourdomain.com,https://www.yourdomain.com
```

### 2. Security Headers
**Status**: ✅ Implemented

**Location**: `SecurityConfig.java:55-69`

**Headers Configured**:
- **Content Security Policy (CSP)**: Restricts resource loading
- **X-XSS-Protection**: Enabled with mode=block
- **X-Frame-Options**: DENY (prevents clickjacking)
- **HSTS**: Enabled with 1-year max-age, includeSubDomains
- **Content-Type Options**: Prevents MIME sniffing

### 3. Rate Limiting
**Status**: ✅ Implemented

**Location**: `RateLimitInterceptor.java`

**Configuration**:
- Authentication endpoints: 10 requests/minute
- Standard endpoints: 100 requests/minute
- Per-client tracking (IP or JWT token)
- HTTP 429 response on limit exceeded

**Dependencies Added**:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

### 4. Input Sanitization
**Status**: ✅ Implemented

**Location**: `InputSanitizer.java`

**Features**:
- HTML content sanitization using OWASP HTML Sanitizer
- File path validation (prevents path traversal)
- Email format validation
- Username sanitization

**Dependencies Added**:
```xml
<dependency>
    <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
    <artifactId>owasp-java-html-sanitizer</artifactId>
    <version>20220608.1</version>
</dependency>
```

**Usage Example**:
```java
@Autowired
private InputSanitizer sanitizer;

// Sanitize HTML content
String clean = sanitizer.sanitizeHtml(userInput);

// Validate file path
String safePath = sanitizer.sanitizeFilePath(uploadPath);

// Validate email
String cleanEmail = sanitizer.sanitizeEmail(email);
```

### 5. Password Validation
**Status**: ✅ Implemented

**Location**: `PasswordValidator.java`

**Requirements**:
- Minimum 8 characters, maximum 100 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character
- No common weak passwords

**Usage Example**:
```java
@Autowired
private PasswordValidator passwordValidator;

try {
    passwordValidator.validate(password);
    // Password is strong
} catch (IllegalArgumentException e) {
    // Password validation failed: e.getMessage()
}
```

## Monitoring Setup

### Architecture
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Backend    │────▶│  Prometheus  │────▶│   Grafana    │
│  (Metrics)   │     │  (Storage)   │     │ (Dashboard)  │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ Alertmanager │
                     │  (Alerts)    │
                     └──────────────┘
```

### Components

#### 1. Prometheus
**Purpose**: Metrics collection and storage

**Configuration**: `monitoring/prometheus/prometheus.yml`

**Scrape Targets**:
- Backend application (port 8080, `/actuator/prometheus`)
- Node exporter (port 9100, system metrics)
- Prometheus itself (port 9090)

**Access**: http://localhost:9090

#### 2. Grafana
**Purpose**: Metrics visualization and dashboards

**Configuration**:
- Datasources: `monitoring/grafana/provisioning/datasources/`
- Dashboards: `monitoring/grafana/provisioning/dashboards/`

**Default Dashboard**: `labelreader-overview.json`

**Panels**:
- Application Status
- Request Rate
- Response Time (95th percentile)
- JVM Memory Usage
- Database Connection Pool
- Error Rate
- CPU Usage

**Access**: http://localhost:3000
**Default Credentials**: admin/admin (change on first login)

#### 3. Alertmanager
**Purpose**: Alert routing and notification

**Configuration**: `monitoring/alertmanager/alertmanager.yml`

**Notification Channels**:
- Email notifications for warnings
- Critical alerts with separate email list

**Alerts Configured**:
- Application Down (2 minutes)
- High Error Rate (>5% for 5 minutes)
- High Response Time (>1s 95th percentile)
- High Memory Usage (>90% JVM heap)
- High CPU Usage (>80% for 10 minutes)
- Database Connection Pool Exhausted (>90%)
- High Rate Limit Hits
- Low Disk Space (<10%)

**Access**: http://localhost:9093

#### 4. Node Exporter
**Purpose**: System metrics collection

**Metrics**:
- CPU usage
- Memory usage
- Disk I/O
- Network statistics

**Access**: http://localhost:9100/metrics

## Deployment

### 1. Start Monitoring Stack

```bash
# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Check status
docker-compose -f docker-compose.monitoring.yml ps

# View logs
docker-compose -f docker-compose.monitoring.yml logs -f grafana
```

### 2. Environment Variables

Create `.env.monitoring` file:

```env
# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=your-secure-password

# Alertmanager Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
ALERT_EMAIL_FROM=alerts@yourdomain.com
ALERT_EMAIL_TO=team@yourdomain.com
ALERT_EMAIL_TO_CRITICAL=oncall@yourdomain.com
```

### 3. Configure Application CORS

Update `.env.production`:

```env
# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com,https://app.yourdomain.com
```

### 4. Build and Deploy

```bash
# Build with new security features
cd apps/backend
mvn clean package

# Deploy with monitoring
docker-compose -f docker-compose.prod.yml up -d
docker-compose -f docker-compose.monitoring.yml up -d
```

## Database Backups

### Automated Backups

#### Setup Cron Jobs

1. Make scripts executable:
```bash
chmod +x scripts/backup/*.sh
```

2. Set environment variables:
```bash
export BACKUP_DIR=/backups/mysql
export DB_PASSWORD=your-db-password
export RETENTION_DAYS=30
```

3. Install crontab:
```bash
crontab scripts/backup/crontab-backup
```

4. Verify installation:
```bash
crontab -l
```

#### Backup Schedule
- **Daily**: 2:00 AM
- **Weekly**: Sunday 3:00 AM
- **Monthly**: 1st of month 4:00 AM

#### Manual Backup

```bash
# Create backup
./scripts/backup/backup-database.sh

# List backups
ls -lh /backups/mysql/

# Restore from backup
./scripts/backup/restore-database.sh /backups/mysql/labelreader_20240101_020000.sql.gz
```

### Backup Storage

**Recommended Locations**:
- Local: `/backups/mysql`
- Network: NAS or network drive
- Cloud: AWS S3, Google Cloud Storage, Azure Blob Storage

**Backup Retention**:
- Daily backups: 7 days
- Weekly backups: 4 weeks
- Monthly backups: 12 months

## Monitoring Access

### Grafana Dashboard

1. Access: http://localhost:3000
2. Login with admin credentials
3. Navigate to "LabelReader Application Overview"

### Prometheus Queries

Access: http://localhost:9090/graph

**Useful Queries**:

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Memory usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Database connections
hikaricp_connections_active
```

## Security Checklist

### Before Production Deployment

- [ ] Update CORS allowed origins for production domains
- [ ] Change default Grafana admin password
- [ ] Configure email alerts with production SMTP
- [ ] Set strong JWT secret (256+ bits)
- [ ] Enable SSL/TLS certificates
- [ ] Configure database backups
- [ ] Test backup restore procedure
- [ ] Review and adjust rate limits
- [ ] Set production logging levels
- [ ] Configure log aggregation
- [ ] Enable firewall rules
- [ ] Set up CDN for static assets
- [ ] Configure database encryption at rest
- [ ] Review and update security headers CSP policy
- [ ] Test all monitoring alerts

### Regular Maintenance

**Weekly**:
- Review Grafana dashboards
- Check backup logs
- Review rate limit alerts
- Update dependencies

**Monthly**:
- Test backup restore
- Review security alerts
- Update Docker images
- Security scan (OWASP Dependency Check)

**Quarterly**:
- Penetration testing
- Security audit
- Disaster recovery drill
- Update security documentation

## Troubleshooting

### Rate Limiting Issues

If legitimate users are being rate-limited:

1. Check current limits in `RateLimitInterceptor.java`
2. Adjust limits based on usage patterns
3. Consider per-user vs per-IP tracking
4. Monitor rate limit metrics in Grafana

### Monitoring Issues

**Prometheus not scraping metrics**:
```bash
# Check backend metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets
# Visit: http://localhost:9090/targets
```

**Grafana dashboard not showing data**:
1. Verify Prometheus datasource: Configuration → Data Sources
2. Check Prometheus query in panel settings
3. Verify time range selection

### Backup Issues

**Backup fails**:
```bash
# Check container status
docker ps | grep labelreader-db

# Check disk space
df -h

# View backup logs
tail -f /var/log/labelreader-backup.log
```

## Performance Considerations

### Rate Limiting Performance

The current in-memory rate limiting implementation is suitable for single-instance deployments. For multi-instance deployments, consider:

- Redis-based rate limiting (distributed)
- Sticky sessions (load balancer level)
- API Gateway with built-in rate limiting

### Monitoring Performance Impact

Prometheus scraping adds minimal overhead:
- Scrape interval: 15 seconds
- Average overhead: <1% CPU, <50MB memory
- Metric storage: ~1KB per metric per sample

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [MySQL Backup Best Practices](https://dev.mysql.com/doc/refman/8.0/en/backup-and-recovery.html)

## Support

For issues or questions:
1. Check logs: `docker-compose logs -f [service-name]`
2. Review metrics in Grafana
3. Check Prometheus alerts
4. Review security audit findings in `SECURITY_AUDIT.md`
