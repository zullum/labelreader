# Performance Optimization Guide

## Overview
This document outlines performance optimization strategies for both backend and frontend of the LabelReader platform.

## Backend Performance Optimization

### 1. Database Query Optimization

#### Add Strategic Indices
```sql
-- Submission queries
CREATE INDEX idx_submissions_artist_status ON submissions(artist_id, submission_status);
CREATE INDEX idx_submissions_genre_rating ON submissions(genre, average_rating DESC);
CREATE INDEX idx_submissions_created_desc ON submissions(created_at DESC);

-- Rating queries
CREATE INDEX idx_ratings_submission_label ON ratings(submission_id, label_id);
CREATE INDEX idx_ratings_label_created ON ratings(label_id, created_at DESC);

-- User queries
CREATE INDEX idx_users_email_active ON users(email, is_active);
CREATE INDEX idx_users_type_created ON users(user_type, created_at DESC);

-- Full-text search
CREATE FULLTEXT INDEX ft_submissions_search ON submissions(title, artist_name, description);
```

#### Query Optimization Tips
```java
// Bad: N+1 Query Problem
List<Submission> submissions = submissionRepository.findAll();
for (Submission submission : submissions) {
    User artist = userRepository.findById(submission.getArtistId()).get();
    // Process...
}

// Good: Join Fetch
@Query("SELECT s FROM Submission s JOIN FETCH s.artist WHERE s.id IN :ids")
List<Submission> findByIdsWithArtist(@Param("ids") List<Long> ids);

// Use pagination
Pageable pageable = PageRequest.of(page, size);
Page<Submission> results = submissionRepository.findAll(pageable);
```

### 2. Caching Strategy

#### Redis Configuration
Add dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Configuration:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("submissions", config.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("users", config.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("ratings", config.entryTtl(Duration.ofMinutes(3)))
            .build();
    }
}
```

#### Cache Implementation
```java
@Service
public class SubmissionService {

    @Cacheable(value = "submissions", key = "#id")
    public SubmissionDto getSubmission(Long id) {
        return submissionRepository.findById(id)
            .map(this::mapToDto)
            .orElseThrow();
    }

    @CacheEvict(value = "submissions", key = "#id")
    public void updateSubmission(Long id, SubmissionRequest request) {
        // Update logic
    }

    @Caching(evict = {
        @CacheEvict(value = "submissions", key = "#submissionId"),
        @CacheEvict(value = "ratings", allEntries = true)
    })
    public void deleteSubmission(Long submissionId) {
        // Delete logic
    }
}
```

### 3. Connection Pool Optimization

```properties
# HikariCP Configuration (application.properties)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000

# Connection pool metrics
spring.datasource.hikari.register-mbeans=true
```

### 4. Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class NotificationService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendEmailNotification(String email, String subject, String body) {
        // Email sending logic
        return CompletableFuture.completedFuture(null);
    }
}
```

### 5. Batch Processing

```java
@Service
public class RatingService {

    @Transactional
    public void batchUpdateSubmissionRatings(List<Long> submissionIds) {
        // Process in batches of 100
        int batchSize = 100;
        for (int i = 0; i < submissionIds.size(); i += batchSize) {
            List<Long> batch = submissionIds.subList(
                i,
                Math.min(i + batchSize, submissionIds.size())
            );

            List<Submission> submissions = submissionRepository.findAllById(batch);
            for (Submission submission : submissions) {
                updateSubmissionRating(submission.getId());
            }

            entityManager.flush();
            entityManager.clear();
        }
    }
}
```

### 6. API Response Compression

```properties
# application.properties
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css
server.compression.min-response-size=1024
```

### 7. Database Connection Optimization

```properties
# JPA Configuration
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
spring.jpa.properties.hibernate.generate_statistics=false

# Query Optimization
spring.jpa.properties.hibernate.query.plan_cache_max_size=2048
spring.jpa.properties.hibernate.query.plan_parameter_metadata_max_size=128
```

## Frontend Performance Optimization

### 1. Lazy Loading Routes

```typescript
// app-routing.module.ts
const routes: Routes = [
  {
    path: 'artist',
    loadChildren: () => import('./artist/artist.module').then(m => m.ArtistModule)
  },
  {
    path: 'label',
    loadChildren: () => import('./label/label.module').then(m => m.LabelModule)
  },
  {
    path: 'discovery',
    loadChildren: () => import('./discovery/discovery.module').then(m => m.DiscoveryModule)
  }
];
```

### 2. OnPush Change Detection

```typescript
@Component({
  selector: 'app-submission-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `...`
})
export class SubmissionCardComponent {
  @Input() submission!: Submission;
}
```

### 3. TrackBy Functions

```typescript
@Component({
  template: `
    <div *ngFor="let item of items; trackBy: trackById">
      {{ item.name }}
    </div>
  `
})
export class ListComponent {
  items: Item[] = [];

  trackById(index: number, item: Item): number {
    return item.id;
  }
}
```

### 4. Virtual Scrolling

```typescript
import { ScrollingModule } from '@angular/cdk/scrolling';

@Component({
  template: `
    <cdk-virtual-scroll-viewport itemSize="100" class="viewport">
      <div *cdkVirtualFor="let submission of submissions">
        <app-submission-card [submission]="submission"></app-submission-card>
      </div>
    </cdk-virtual-scroll-viewport>
  `,
  styles: [`
    .viewport {
      height: 600px;
      width: 100%;
    }
  `]
})
export class SubmissionListComponent {
  submissions: Submission[] = [];
}
```

### 5. Image Optimization

```typescript
// Use loading="lazy" for images
<img
  [src]="submission.coverImage"
  loading="lazy"
  [alt]="submission.title"
  width="300"
  height="300"
>

// Implement responsive images
<img
  [srcset]="submission.coverImage + ' 1x, ' + submission.coverImageHD + ' 2x'"
  [src]="submission.coverImage"
  loading="lazy"
>
```

### 6. Bundle Size Optimization

```json
// angular.json
{
  "projects": {
    "labelreader-frontend": {
      "architect": {
        "build": {
          "configurations": {
            "production": {
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "vendorChunk": false,
              "buildOptimizer": true,
              "budgets": [
                {
                  "type": "initial",
                  "maximumWarning": "2mb",
                  "maximumError": "5mb"
                },
                {
                  "type": "anyComponentStyle",
                  "maximumWarning": "6kb",
                  "maximumError": "10kb"
                }
              ]
            }
          }
        }
      }
    }
  }
}
```

### 7. Service Worker & PWA

```bash
ng add @angular/pwa
```

```typescript
// app.module.ts
import { ServiceWorkerModule } from '@angular/service-worker';

@NgModule({
  imports: [
    ServiceWorkerModule.register('ngsw-worker.js', {
      enabled: environment.production,
      registrationStrategy: 'registerWhenStable:30000'
    })
  ]
})
export class AppModule { }
```

### 8. HTTP Caching

```typescript
@Injectable()
export class CacheInterceptor implements HttpInterceptor {
  private cache = new Map<string, HttpResponse<any>>();

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.method !== 'GET') {
      return next.handle(req);
    }

    const cachedResponse = this.cache.get(req.url);
    if (cachedResponse) {
      return of(cachedResponse);
    }

    return next.handle(req).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          this.cache.set(req.url, event);
        }
      })
    );
  }
}
```

### 9. Debounce Search Input

```typescript
@Component({
  template: `
    <input
      type="text"
      [formControl]="searchControl"
      placeholder="Search..."
    >
  `
})
export class SearchComponent implements OnInit {
  searchControl = new FormControl('');

  ngOnInit() {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => this.searchService.search(query))
      )
      .subscribe(results => {
        this.searchResults = results;
      });
  }
}
```

### 10. Preloading Strategy

```typescript
@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      preloadingStrategy: PreloadAllModules
    })
  ]
})
export class AppModule { }
```

## Performance Monitoring

### Backend Monitoring

#### Spring Boot Actuator + Prometheus
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

#### JVM Metrics
```java
@Configuration
public class MonitoringConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "labelreader-backend");
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class SubmissionService {

    @Timed(value = "submission.get", description = "Time taken to get submission")
    public SubmissionDto getSubmission(Long id) {
        // Implementation
    }
}
```

### Frontend Monitoring

#### Angular Performance API
```typescript
@Injectable()
export class PerformanceService {

  measureComponentLoad(componentName: string) {
    performance.mark(`${componentName}-start`);
  }

  finishMeasurement(componentName: string) {
    performance.mark(`${componentName}-end`);
    performance.measure(
      componentName,
      `${componentName}-start`,
      `${componentName}-end`
    );

    const measure = performance.getEntriesByName(componentName)[0];
    console.log(`${componentName} loaded in ${measure.duration}ms`);
  }
}
```

## Performance Benchmarks

### Target Metrics

#### Backend
- API Response Time (p95): < 500ms
- Database Query Time (p95): < 100ms
- File Upload Time (50MB): < 10s
- Throughput: > 1000 req/s

#### Frontend
- First Contentful Paint: < 1.5s
- Largest Contentful Paint: < 2.5s
- Time to Interactive: < 3.5s
- Cumulative Layout Shift: < 0.1
- First Input Delay: < 100ms

#### Database
- Connection Pool Utilization: < 80%
- Query Cache Hit Rate: > 90%
- Index Usage: > 95%

## Performance Testing Tools

### Load Testing
```bash
# Apache JMeter
jmeter -n -t load-test.jmx -l results.jtl

# Artillery
artillery run load-test.yml

# k6
k6 run load-test.js
```

### Profiling
```bash
# Java Flight Recorder
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr

# Chrome DevTools (Frontend)
# Use Performance tab for profiling
```

## Continuous Performance Monitoring

1. Set up Grafana dashboards
2. Configure alerts for performance degradation
3. Regular performance audits
4. A/B testing for optimization changes
5. User experience monitoring (Lighthouse CI)

## Optimization Checklist

### Before Production
- [ ] Database indices optimized
- [ ] Query performance tested
- [ ] Caching strategy implemented
- [ ] Connection pool tuned
- [ ] Frontend bundle optimized
- [ ] Images optimized
- [ ] Lazy loading implemented
- [ ] Service worker configured
- [ ] Performance monitoring setup
- [ ] Load testing completed

### Regular Maintenance
- [ ] Weekly performance review
- [ ] Monthly load testing
- [ ] Quarterly optimization audit
- [ ] Database query analysis
- [ ] Cache hit rate monitoring
