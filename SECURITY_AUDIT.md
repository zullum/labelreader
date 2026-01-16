# Security Audit & Checklist

## Overview
This document provides a comprehensive security audit checklist and recommendations for the LabelReader platform.

## Security Audit Status

### Authentication & Authorization
- [x] JWT-based authentication implemented
- [x] Password hashing with BCrypt (strength: 10)
- [x] Token expiration configured (Access: 15min, Refresh: 7 days)
- [ ] Rate limiting on auth endpoints (RECOMMENDED)
- [ ] Account lockout after failed attempts (RECOMMENDED)
- [ ] Two-factor authentication (FUTURE)
- [x] Role-based access control (ARTIST/LABEL)
- [ ] Password complexity requirements (RECOMMENDED)
- [ ] Password reset functionality (REQUIRED)

### Input Validation
- [x] Request validation with @Valid annotations
- [x] File type validation for uploads
- [x] File size limits (50MB)
- [ ] XSS prevention in frontend (REQUIRED)
- [ ] SQL injection protection (JPA handles this)
- [ ] Path traversal prevention in file uploads (REQUIRED)
- [ ] Email format validation (REQUIRED)
- [ ] Sanitize user-generated content (REQUIRED)

### Data Protection
- [x] Passwords never stored in plain text
- [x] JWT secrets environment-based
- [ ] Database encryption at rest (RECOMMENDED)
- [ ] SSL/TLS for all communications (REQUIRED for production)
- [ ] Sensitive data masking in logs (REQUIRED)
- [ ] GDPR compliance measures (REQUIRED)
- [ ] Data retention policies (RECOMMENDED)
- [ ] Secure file storage permissions (REQUIRED)

### API Security
- [ ] CORS configuration (REQUIRED)
- [ ] CSRF protection (REQUIRED)
- [ ] Rate limiting per endpoint (RECOMMENDED)
- [ ] Request size limits (RECOMMENDED)
- [ ] API versioning (RECOMMENDED)
- [ ] Security headers (REQUIRED)
- [ ] Input sanitization (REQUIRED)
- [ ] Output encoding (REQUIRED)

### Infrastructure Security
- [x] Docker container isolation
- [x] Environment variable configuration
- [ ] Secrets management (REQUIRED)
- [ ] Network segmentation (RECOMMENDED)
- [ ] Firewall rules (REQUIRED)
- [ ] DDoS protection (RECOMMENDED)
- [ ] Regular security updates (REQUIRED)
- [ ] Container scanning (RECOMMENDED)

## Critical Security Fixes Required

### 1. CORS Configuration
**Priority: HIGH**

Add to Spring Boot configuration:

```java
@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "https://yourdomain.com",
            "https://www.yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### 2. Rate Limiting
**Priority: HIGH**

Add Bucket4j dependency:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

Implement rate limiting:
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(429); // Too Many Requests
            return false;
        }
    }

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
            .build();
    }
}
```

### 3. Security Headers
**Priority: HIGH**

```java
@Configuration
public class SecurityHeadersConfig {

    @Bean
    public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"))
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .contentTypeOptions(Customizer.withDefaults())
            .frameOptions(frame -> frame.deny())
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
                .preload(true))
        );
        return http.build();
    }
}
```

### 4. Input Sanitization
**Priority: HIGH**

```java
@Component
public class InputSanitizer {

    private final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    public String sanitizeHtml(String input) {
        return policy.sanitize(input);
    }

    public String sanitizeFilePath(String filePath) {
        // Prevent path traversal
        String normalized = Paths.get(filePath).normalize().toString();
        if (normalized.contains("..")) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return normalized;
    }
}
```

Add dependency:
```xml
<dependency>
    <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
    <artifactId>owasp-java-html-sanitizer</artifactId>
    <version>20220608.1</version>
</dependency>
```

### 5. File Upload Security
**Priority: CRITICAL**

```java
@Service
public class SecureFileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "audio/mpeg", "audio/wav", "audio/x-wav", "audio/flac", "audio/x-flac"
    );

    private static final long MAX_SIZE = 50 * 1024 * 1024; // 50MB

    public void validateFile(MultipartFile file) {
        // Check size
        if (file.getSize() > MAX_SIZE) {
            throw new SecurityException("File size exceeds limit");
        }

        // Check content type
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new SecurityException("Invalid file type");
        }

        // Verify magic bytes
        try {
            byte[] bytes = file.getBytes();
            if (!isValidAudioFile(bytes)) {
                throw new SecurityException("File content doesn't match declared type");
            }
        } catch (IOException e) {
            throw new SecurityException("Failed to read file", e);
        }

        // Sanitize filename
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/"))) {
            throw new SecurityException("Invalid filename");
        }
    }

    private boolean isValidAudioFile(byte[] bytes) {
        // Check MP3 magic bytes
        if (bytes.length >= 3 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFB) {
            return true;
        }
        // Check WAV magic bytes
        if (bytes.length >= 4 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F') {
            return true;
        }
        // Check FLAC magic bytes
        if (bytes.length >= 4 && bytes[0] == 'f' && bytes[1] == 'L' && bytes[2] == 'a' && bytes[3] == 'C') {
            return true;
        }
        return false;
    }
}
```

### 6. Password Policy
**Priority: HIGH**

```java
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public void validate(String password) {
        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (!UPPERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain uppercase letter");
        }

        if (!LOWERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain lowercase letter");
        }

        if (!DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain digit");
        }

        if (!SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain special character");
        }
    }
}
```

### 7. Logging Security
**Priority: MEDIUM**

```java
@Aspect
@Component
public class SensitiveDataMaskingAspect {

    private static final Pattern EMAIL = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    @Around("execution(* com.labelreader..*(..))")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                args[i] = maskEmail((String) args[i]);
            }
        }
        return joinPoint.proceed(args);
    }

    private String maskEmail(String input) {
        if (input == null) return null;
        Matcher matcher = EMAIL.matcher(input);
        return matcher.replaceAll("$1****@$2");
    }
}
```

## Security Testing

### Automated Security Scans

#### 1. OWASP Dependency Check
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Run: `mvn dependency-check:check`

#### 2. Trivy Container Scanning
```bash
trivy image labelreader-backend:latest
trivy image labelreader-frontend:latest
```

#### 3. SonarQube Analysis
```bash
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

### Manual Security Testing

#### Penetration Testing Checklist
- [ ] SQL Injection attempts
- [ ] XSS attacks
- [ ] CSRF attacks
- [ ] Authentication bypass
- [ ] Authorization bypass
- [ ] Session fixation
- [ ] File upload exploits
- [ ] Path traversal
- [ ] Command injection
- [ ] XML external entity (XXE)

#### Tools
- OWASP ZAP
- Burp Suite
- Postman (API testing)
- SQLMap
- Nikto

## Production Security Checklist

### Before Deployment
- [ ] All secrets in environment variables
- [ ] SSL/TLS certificates installed
- [ ] Security headers configured
- [ ] CORS properly configured
- [ ] Rate limiting enabled
- [ ] Input validation complete
- [ ] File upload security implemented
- [ ] Logging configured (no sensitive data)
- [ ] Database backups configured
- [ ] Incident response plan ready

### After Deployment
- [ ] Security scan completed
- [ ] Penetration test performed
- [ ] Monitoring alerts configured
- [ ] Access logs reviewed
- [ ] Security patches applied
- [ ] Disaster recovery tested

## Compliance

### GDPR Requirements
- [ ] Privacy policy published
- [ ] Cookie consent implemented
- [ ] Data export functionality
- [ ] Right to deletion (data erasure)
- [ ] Data processing agreements
- [ ] Breach notification process

### OWASP Top 10 Coverage
1. ✅ Broken Access Control - Role-based access implemented
2. ✅ Cryptographic Failures - BCrypt for passwords, JWT for tokens
3. ⚠️ Injection - Need input sanitization
4. ⚠️ Insecure Design - Need security reviews
5. ⚠️ Security Misconfiguration - Need security headers
6. ✅ Vulnerable Components - Dependency scanning in CI/CD
7. ⚠️ Authentication Failures - Need rate limiting
8. ✅ Software and Data Integrity - Signed containers
9. ⚠️ Security Logging Failures - Need security event logging
10. ⚠️ Server-Side Request Forgery - Need URL validation

## Incident Response

### Security Incident Procedure
1. **Detect** - Monitor logs and alerts
2. **Contain** - Isolate affected systems
3. **Investigate** - Determine scope and impact
4. **Remediate** - Fix vulnerabilities
5. **Recover** - Restore services
6. **Review** - Post-incident analysis

### Emergency Contacts
- Security Team Lead: [Contact]
- Infrastructure Team: [Contact]
- Legal/Compliance: [Contact]

## Regular Security Maintenance

### Weekly
- Review access logs
- Check security alerts
- Update dependencies

### Monthly
- Security scan
- Review user permissions
- Audit API usage

### Quarterly
- Penetration testing
- Security training
- Disaster recovery drill
- Update security documentation

## Resources
- OWASP Top 10: https://owasp.org/www-project-top-ten/
- Spring Security: https://spring.io/projects/spring-security
- NIST Cybersecurity Framework: https://www.nist.gov/cyberframework
