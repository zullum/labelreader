# LabelReader - Production Deployment Quick Start

## Prerequisites
- Docker & Docker Compose installed
- SSL certificates for HTTPS
- MySQL 8.2+ compatible server
- Node.js 22 LTS & JDK 21
- Domain name configured with DNS

## Quick Deployment (5 Steps)

### 1. Clone & Configure
```bash
git clone <repository-url>
cd labelreader

# Copy environment template
cp .env.production.example .env.production

# Edit with your values
nano .env.production
```

**Required Environment Variables:**
```bash
# Database
DB_ROOT_PASSWORD=<strong-password>
DB_PASSWORD=<strong-password>

# JWT (Generate: openssl rand -base64 32)
JWT_SECRET=<256-bit-secret>

# Email
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=<your-email>
MAIL_PASSWORD=<app-password>

# Domain
API_URL=https://api.yourdomain.com
FRONTEND_URL=https://yourdomain.com
```

### 2. Prepare SSL Certificates
```bash
mkdir -p nginx/ssl
# Place your certificates
cp /path/to/cert.pem nginx/ssl/
cp /path/to/key.pem nginx/ssl/
```

### 3. Build & Deploy
```bash
# Build images
docker-compose -f docker-compose.prod.yml build

# Start services
docker-compose -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.prod.yml ps
```

### 4. Run Database Migrations
```bash
# Wait for services to be healthy (30-60 seconds)
docker-compose -f docker-compose.prod.yml logs -f backend

# Migrations run automatically on startup
# Verify in logs: "Flyway: Successfully applied X migration(s)"
```

### 5. Verify Deployment
```bash
# Health checks
curl https://api.yourdomain.com/actuator/health
curl https://yourdomain.com/health

# Test endpoints
curl https://api.yourdomain.com/api/auth/health
```

## Monitoring

### View Logs
```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### Health Checks
```bash
# Backend health
curl http://localhost:8080/actuator/health

# Check all containers
docker-compose -f docker-compose.prod.yml ps
```

### Database Access
```bash
docker exec -it labelreader-db-prod mysql -u root -p
```

## Backup & Restore

### Backup Database
```bash
# Manual backup
docker exec labelreader-db-prod mysqldump \
  -u root -p$DB_ROOT_PASSWORD labelreader \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Automated daily backup (crontab)
0 2 * * * /path/to/backup-script.sh
```

### Restore Database
```bash
# Stop backend first
docker-compose -f docker-compose.prod.yml stop backend

# Restore
docker exec -i labelreader-db-prod mysql \
  -u root -p$DB_ROOT_PASSWORD labelreader \
  < backup_YYYYMMDD_HHMMSS.sql

# Restart backend
docker-compose -f docker-compose.prod.yml start backend
```

## Troubleshooting

### Services Won't Start
```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs

# Verify environment variables
docker-compose -f docker-compose.prod.yml config

# Restart services
docker-compose -f docker-compose.prod.yml restart
```

### Database Connection Issues
```bash
# Check MySQL health
docker-compose -f docker-compose.prod.yml exec mysql mysqladmin ping -u root -p

# Verify network
docker-compose -f docker-compose.prod.yml exec backend ping mysql

# Check credentials
docker-compose -f docker-compose.prod.yml exec backend env | grep DB_
```

### High Memory Usage
```bash
# Check resource usage
docker stats

# Restart specific service
docker-compose -f docker-compose.prod.yml restart backend

# Scale down if needed (edit docker-compose.prod.yml)
# Adjust: deploy.resources.limits.memory
```

### SSL Certificate Issues
```bash
# Verify certificate files
ls -la nginx/ssl/

# Check nginx configuration
docker-compose -f docker-compose.prod.yml exec frontend nginx -t

# Reload nginx
docker-compose -f docker-compose.prod.yml exec frontend nginx -s reload
```

## Maintenance

### Update Application
```bash
# Pull latest images
docker-compose -f docker-compose.prod.yml pull

# Restart with new images
docker-compose -f docker-compose.prod.yml up -d --no-deps backend frontend

# Verify
docker-compose -f docker-compose.prod.yml ps
```

### Clean Up
```bash
# Remove stopped containers
docker-compose -f docker-compose.prod.yml down

# Remove old images
docker image prune -a

# Remove unused volumes (careful!)
docker volume prune
```

### Security Updates
```bash
# Update base images
docker-compose -f docker-compose.prod.yml build --pull

# Scan for vulnerabilities
trivy image labelreader-backend:latest
trivy image labelreader-frontend:latest
```

## Performance Tuning

### Database Optimization
```sql
-- Add missing indices
CREATE INDEX idx_submissions_genre_status ON submissions(genre, submission_status);
CREATE INDEX idx_ratings_submission_label ON ratings(submission_id, label_id);

-- Analyze tables
ANALYZE TABLE submissions;
ANALYZE TABLE ratings;
ANALYZE TABLE users;
```

### Backend Tuning
Edit `application.properties`:
```properties
# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JPA optimization
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

### Frontend Optimization
Already configured in production build:
- Tree shaking enabled
- AOT compilation
- Bundle optimization
- Lazy loading routes

## Security Checklist

### Before Going Live
- [ ] Change all default passwords
- [ ] Generate strong JWT secret (256-bit)
- [ ] Enable SSL/TLS (HTTPS only)
- [ ] Configure firewall rules
- [ ] Set up rate limiting
- [ ] Enable CORS with specific origins
- [ ] Review security headers
- [ ] Scan for vulnerabilities
- [ ] Set up automated backups
- [ ] Configure monitoring alerts

### Post-Deployment
- [ ] Test authentication flow
- [ ] Verify file upload security
- [ ] Check API rate limits
- [ ] Review access logs
- [ ] Monitor error rates
- [ ] Verify backup jobs
- [ ] Test disaster recovery

## CI/CD Integration

### GitHub Actions Setup
1. Add secrets to repository:
   - `PROD_HOST`
   - `PROD_USERNAME`
   - `PROD_SSH_KEY`
   - `PROD_PORT`
   - `PROD_URL`
   - `SLACK_WEBHOOK` (optional)

2. Push to main branch triggers:
   - Automated tests
   - Security scanning
   - Image building
   - Production deployment

### Manual Deployment
```bash
# Build locally
docker-compose -f docker-compose.prod.yml build

# Push to registry
docker tag labelreader-backend:latest your-registry/labelreader-backend:latest
docker push your-registry/labelreader-backend:latest

# Deploy on server
ssh user@server 'cd /opt/labelreader && docker-compose pull && docker-compose up -d'
```

## Support & Documentation

### Documentation Files
- `DATABASE_MIGRATION.md` - Migration procedures
- `SECURITY_AUDIT.md` - Security best practices
- `PERFORMANCE_OPTIMIZATION.md` - Performance tuning
- `PHASE5_COMPLETION_SUMMARY.md` - Implementation details

### Running Tests
```bash
# Backend tests
cd apps/backend && mvn test

# E2E tests
cd e2e-tests && npm test

# Frontend tests (when implemented)
cd apps/frontend && npm test
```

### Getting Help
- Check logs: `docker-compose logs -f`
- Review documentation in repository
- Verify environment variables
- Check GitHub issues

## Success Criteria

✅ All services running and healthy
✅ Database migrations completed
✅ SSL certificates valid
✅ API endpoints responding
✅ Frontend loading correctly
✅ Authentication working
✅ File uploads functional
✅ Email notifications sending
✅ Backups configured
✅ Monitoring active

## Emergency Contacts

- Technical Lead: [Contact Info]
- DevOps Team: [Contact Info]
- Database Admin: [Contact Info]
- Security Team: [Contact Info]

## Quick Commands Reference

```bash
# Start
docker-compose -f docker-compose.prod.yml up -d

# Stop
docker-compose -f docker-compose.prod.yml down

# Restart service
docker-compose -f docker-compose.prod.yml restart backend

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Check status
docker-compose -f docker-compose.prod.yml ps

# Backup database
docker exec labelreader-db-prod mysqldump -u root -p labelreader > backup.sql

# Scale service
docker-compose -f docker-compose.prod.yml up -d --scale backend=2

# Update & restart
docker-compose -f docker-compose.prod.yml pull && \
docker-compose -f docker-compose.prod.yml up -d
```

---

**Deployment Time:** ~15-30 minutes
**Prerequisite Setup:** ~1-2 hours
**Total Time to Production:** 2-3 hours

**Platform Status:** ✅ Production Ready
