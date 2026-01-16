# Security & Monitoring Features

## 🔒 Security Features

### Rate Limiting
Protects against brute force attacks and API abuse:
- **Authentication endpoints**: 10 requests/minute
- **Standard endpoints**: 100 requests/minute
- **Response**: HTTP 429 with retry-after header

### CORS Configuration
Prevents unauthorized cross-origin requests:
- Configurable allowed origins
- Restricted methods and headers
- Credential support enabled

### Security Headers
Comprehensive browser-level protection:
- Content Security Policy (CSP)
- X-XSS-Protection
- X-Frame-Options (DENY)
- HTTP Strict Transport Security (HSTS)
- X-Content-Type-Options

### Input Sanitization
Prevents injection attacks:
- HTML/XSS sanitization
- Path traversal prevention
- Email validation
- Username sanitization

### Password Policy
Strong password requirements:
- Minimum 8 characters
- Mixed case, digits, special characters
- Blocks common weak passwords

## 📊 Monitoring Stack

### Prometheus
- **URL**: http://localhost:9090
- **Scrape interval**: 15 seconds
- **Retention**: 30 days
- **Metrics**: Application, JVM, database, system

### Grafana
- **URL**: http://localhost:3000
- **Credentials**: admin/admin (change on first login)
- **Dashboard**: LabelReader Application Overview
- **Panels**: Request rate, response time, memory, CPU, errors, DB connections

### Alertmanager
- **URL**: http://localhost:9093
- **Notifications**: Email alerts for critical issues
- **Alerts**: 8 configured rules

### Key Metrics
- Request rate and latency
- Error rates (4xx, 5xx)
- JVM memory and CPU usage
- Database connection pool
- Rate limit hits
- System resources

## 🗄️ Database Backups

### Automated Backups
- **Daily**: 2:00 AM
- **Weekly**: Sunday 3:00 AM
- **Monthly**: 1st of month 4:00 AM
- **Retention**: 30 days (configurable)

### Manual Operations
```bash
# Create backup
./scripts/backup/backup-database.sh

# List backups
ls -lh /backups/mysql/

# Restore backup
./scripts/backup/restore-database.sh /backups/mysql/backup_file.sql.gz
```

## 🚀 Quick Start

### 1. Configure Environment
```bash
cp .env.production.example .env.production
# Edit with your values
```

### 2. Deploy Application
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 3. Deploy Monitoring
```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

### 4. Set Up Backups
```bash
chmod +x scripts/backup/*.sh
crontab scripts/backup/crontab-backup
```

### 5. Access Dashboards
- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **Alertmanager**: http://localhost:9093

## 📚 Documentation

- **Comprehensive Guide**: `SECURITY_SETUP_GUIDE.md`
- **Quick Start**: `QUICKSTART_SECURITY.md`
- **Implementation Details**: `PHASE6_SECURITY_MONITORING.md`
- **Security Audit**: `SECURITY_AUDIT.md`

## ✅ Production Checklist

Before deploying to production:

- [ ] Change all default passwords
- [ ] Configure CORS for production domains
- [ ] Set up SSL/TLS certificates
- [ ] Configure email alerts
- [ ] Test backup and restore
- [ ] Verify rate limiting
- [ ] Check security headers
- [ ] Review monitoring dashboards
- [ ] Test alerting system
- [ ] Configure log aggregation

## 🔧 Troubleshooting

### Rate Limiting Issues
```bash
# Check logs
docker-compose logs backend | grep -i "rate limit"

# Test limit
for i in {1..15}; do curl http://localhost:8080/api/auth/login; done
```

### Monitoring Issues
```bash
# Check Prometheus targets
# Visit: http://localhost:9090/targets

# Check metrics endpoint
curl http://localhost:8080/actuator/prometheus
```

### Backup Issues
```bash
# Test backup manually
./scripts/backup/backup-database.sh

# Check backup logs
tail -f /var/log/labelreader-backup.log
```

## 🎯 Key Endpoints

### Application
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`
- Info: `http://localhost:8080/actuator/info`

### Monitoring
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Alertmanager: `http://localhost:9093`
- Node Exporter: `http://localhost:9100/metrics`

## 📈 Monitoring Queries

Useful Prometheus queries:

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Memory usage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Database connections
hikaricp_connections_active
```

## 🔐 Security Best Practices

1. **Regularly update dependencies**: `mvn versions:display-dependency-updates`
2. **Review security logs**: Check Grafana dashboards daily
3. **Test backups**: Monthly restore test
4. **Monitor alerts**: Review Alertmanager notifications
5. **Update CORS**: Keep allowed origins list minimal
6. **Rotate secrets**: JWT secret, database passwords quarterly
7. **Security scanning**: Run OWASP dependency check monthly
8. **Penetration testing**: Quarterly security assessment

## 📞 Support

For issues or questions:
1. Check logs: `docker-compose logs -f [service-name]`
2. Review Grafana dashboards
3. Check Prometheus alerts
4. Consult `SECURITY_SETUP_GUIDE.md`
5. Review `TROUBLESHOOTING.md`

## 🎉 Features Implemented

- ✅ CORS configuration
- ✅ Rate limiting (Bucket4j)
- ✅ Security headers
- ✅ Input sanitization (OWASP)
- ✅ Password validation
- ✅ Prometheus metrics
- ✅ Grafana dashboards
- ✅ Alertmanager notifications
- ✅ Automated backups
- ✅ Node exporter metrics

---

**Last Updated**: December 2, 2025
**Version**: 1.0.0
**Status**: Production Ready 🚀
