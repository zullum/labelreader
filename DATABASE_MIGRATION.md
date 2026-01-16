# Database Migration Strategy

## Overview
This document outlines the database migration strategy for the LabelReader platform, covering both development and production environments.

## Migration Tools

### Flyway (Recommended)
We use Flyway for database migrations, integrated with Spring Boot.

#### Setup
Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Add to `application.properties`:
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.sql-migration-prefix=V
spring.flyway.sql-migration-separator=__
spring.flyway.sql-migration-suffixes=.sql
```

## Migration File Structure

```
apps/backend/src/main/resources/
└── db/
    ├── migration/
    │   ├── V1__Initial_Schema.sql
    │   ├── V2__Add_Notifications_Table.sql
    │   ├── V3__Add_Play_History_Table.sql
    │   └── V4__Add_Analytics_Indices.sql
    └── init.sql (for Docker initial setup)
```

## Migration Naming Convention

Format: `V{version}__{description}.sql`

Examples:
- `V1__Initial_Schema.sql` - Initial database schema
- `V2__Add_User_Verification_Column.sql` - Add new column
- `V3__Alter_Submission_Status_Values.sql` - Alter enum values
- `V4__Create_Analytics_Indices.sql` - Add performance indices

## Migration Process

### Development Environment
1. Create migration file in `db/migration/`
2. Follow naming convention
3. Test migration locally:
   ```bash
   mvn flyway:migrate
   ```
4. Verify changes:
   ```bash
   mvn flyway:info
   ```

### Production Environment

#### Pre-Migration Checklist
- [ ] Backup database
- [ ] Test migration on staging environment
- [ ] Review rollback plan
- [ ] Schedule maintenance window
- [ ] Notify team members

#### Migration Steps
1. **Backup Database**
   ```bash
   docker exec labelreader-db-prod mysqldump -u root -p labelreader > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Stop Application** (if necessary for breaking changes)
   ```bash
   docker-compose -f docker-compose.prod.yml stop backend
   ```

3. **Run Migration**
   ```bash
   docker-compose -f docker-compose.prod.yml exec backend \
     mvn flyway:migrate -Dflyway.url=jdbc:mysql://mysql:3306/labelreader \
     -Dflyway.user=labelreader_user -Dflyway.password=$DB_PASSWORD
   ```

4. **Verify Migration**
   ```bash
   docker-compose -f docker-compose.prod.yml exec backend mvn flyway:info
   ```

5. **Start Application**
   ```bash
   docker-compose -f docker-compose.prod.yml start backend
   ```

6. **Health Check**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Rollback Strategy

#### Option 1: Restore from Backup
```bash
docker exec -i labelreader-db-prod mysql -u root -p labelreader < backup_YYYYMMDD_HHMMSS.sql
```

#### Option 2: Manual Rollback Script
Create rollback scripts for each migration:
- `V2__Add_Notifications_Table.sql` → `U2__Rollback_Notifications_Table.sql`

Example rollback script:
```sql
-- U2__Rollback_Notifications_Table.sql
DROP TABLE IF EXISTS notifications;
```

## Migration Best Practices

### DO
- ✅ Always create backups before migrations
- ✅ Test migrations on staging first
- ✅ Use transactions when possible
- ✅ Add indices for performance
- ✅ Document complex changes
- ✅ Include rollback scripts
- ✅ Use descriptive migration names
- ✅ Keep migrations small and focused

### DON'T
- ❌ Never edit existing migration files
- ❌ Don't delete migration files
- ❌ Avoid complex logic in migrations
- ❌ Don't skip version numbers
- ❌ Never run migrations manually in production without testing
- ❌ Don't use DROP TABLE without backup
- ❌ Avoid mixing DDL and DML in same migration

## Common Migration Scenarios

### 1. Adding a New Column
```sql
-- V5__Add_User_Bio_Column.sql
ALTER TABLE users ADD COLUMN bio TEXT AFTER country;
```

### 2. Creating a New Table
```sql
-- V6__Create_User_Sessions_Table.sql
CREATE TABLE user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB;
```

### 3. Adding an Index
```sql
-- V7__Add_Submission_Performance_Indices.sql
CREATE INDEX idx_submissions_genre_status ON submissions(genre, submission_status);
CREATE INDEX idx_submissions_created_rating ON submissions(created_at, average_rating);
```

### 4. Modifying Data
```sql
-- V8__Normalize_Genre_Values.sql
UPDATE submissions SET genre = 'Electronic' WHERE genre IN ('electronic', 'ELECTRONIC', 'Electronic Music');
UPDATE submissions SET genre = 'Hip Hop' WHERE genre IN ('hip hop', 'Hip-Hop', 'HipHop');
```

### 5. Altering Column Type
```sql
-- V9__Increase_Review_Text_Length.sql
ALTER TABLE ratings MODIFY COLUMN review_text TEXT;
```

## Automated Migration in CI/CD

GitHub Actions workflow includes automatic migration:

```yaml
- name: Run database migrations
  run: |
    docker-compose -f docker-compose.prod.yml exec -T backend \
      mvn flyway:migrate
```

## Monitoring Migrations

### Check Migration Status
```bash
mvn flyway:info
```

### Validate Migrations
```bash
mvn flyway:validate
```

### Repair Migrations (use with caution)
```bash
mvn flyway:repair
```

## Emergency Procedures

### If Migration Fails Mid-Execution
1. Check Flyway schema history:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```

2. Mark failed migration as failed:
   ```bash
   mvn flyway:repair
   ```

3. Restore from backup if necessary

4. Investigate root cause

5. Fix migration script

6. Re-run migration

## Database Backup Strategy

### Automated Backups
```bash
# Add to crontab
0 2 * * * docker exec labelreader-db-prod mysqldump -u root -p$DB_ROOT_PASSWORD labelreader | gzip > /backups/labelreader_$(date +\%Y\%m\%d_\%H\%M\%S).sql.gz
```

### Backup Retention
- Daily backups: 7 days
- Weekly backups: 4 weeks
- Monthly backups: 12 months

### Backup Verification
```bash
# Test restore on staging
gunzip < backup.sql.gz | docker exec -i labelreader-db-staging mysql -u root -p labelreader
```

## Schema Versioning

Current schema version is tracked in:
- Flyway: `flyway_schema_history` table
- Application: `application.properties` schema version property

## Documentation

Maintain migration log:
- Migration version
- Date applied
- Applied by
- Purpose
- Rollback tested: Yes/No
- Production result: Success/Failed

## Contact

For migration issues:
- Development: Check application logs
- Production: Contact DevOps team
- Emergency: Follow incident response procedure
