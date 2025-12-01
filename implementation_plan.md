# Music Platform - Technical Implementation Plan

## Project Overview

A comprehensive music submission and review platform connecting independent artists with record labels. Artists can upload their music works, while labels can discover, review, rate, and decide on potential releases.

### Technology Stack (December 2025)

| Component                   | Technology     | Version       |
| --------------------------- | -------------- | ------------- |
| **Frontend**                | Angular        | 21.0.1        |
| **Backend**                 | Spring Boot    | 3.4.0         |
| **Database**                | MySQL          | 8.2+ (latest) |
| **Runtime**                 | Node.js        | 22 LTS        |
| **Java**                    | OpenJDK        | 21 LTS        |
| **Build Tool (BE)**         | Maven          | 3.9+          |
| **Containerization**        | Docker         | 24+           |
| **Container Orchestration** | Docker Compose | 2.23+         |
| **Monorepo Tool**           | Nx             | 20+           |

### Design Reference

![LabelRadar Design Reference](/Users/sanel.zulic/.gemini/antigravity/brain/07b410d7-ac92-418d-a734-aaa92d9a65e1/uploaded_image_1764599889242.png)

**Color Palette:**

- Primary Background: `#1a2e3d` (Dark Teal/Navy)
- Secondary Background: `#234253` (Medium Teal)
- Accent/CTA: `#4ecca3` (Green)
- Text Primary: `#ffffff` (White)
- Text Secondary: `#a0aec0` (Light Gray)

---

## Project Structure

```
labelreader/
├── .nx/                          # Nx cache and configuration
├── apps/
│   ├── frontend/                 # Angular application
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── nginx.conf
│   └── backend/                  # Spring Boot application
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── libs/                         # Shared libraries
│   ├── shared-models/            # TypeScript/Java shared models
│   └── shared-utils/
├── docker/
│   ├── docker-compose.yml        # Main orchestration
│   ├── docker-compose.dev.yml    # Development overrides
│   └── docker-compose.prod.yml   # Production overrides
├── docs/                         # Documentation
├── scripts/                      # Build and deployment scripts
├── nx.json                       # Nx workspace configuration
├── package.json                  # Root package.json
└── README.md
```

---

## User Review Required

> [!IMPORTANT] > **File Upload & Storage Strategy**
>
> This plan assumes audio files will be stored on the server's filesystem with metadata in MySQL. For production, consider:
>
> - **Cloud Storage:** AWS S3, Google Cloud Storage, or Azure Blob Storage for scalability
> - **CDN:** CloudFront or similar for global distribution
> - **Max File Size:** Currently set to 50MB per track
>
> Please confirm storage strategy before Phase 2 implementation.

> [!WARNING] > **Authentication Method**
>
> Currently planning JWT-based authentication with:
>
> - Access tokens (15 min expiry)
> - Refresh tokens (7 day expiry)
>
> If you require OAuth2 integration (Google, Spotify, etc.), this should be planned in Phase 1.

> [!IMPORTANT] > **Audio Playback**
>
> Frontend will use HTML5 Audio API for playback. Waveform visualization will use WaveSurfer.js. Confirm if you need advanced features like:
>
> - Real-time streaming
> - Audio fingerprinting
> - Automatic genre detection

---

## Phase 1: Foundation & Infrastructure (Sprint 1-2)

**Goal:** Set up development environment, monorepo structure, Docker containers, and basic authentication.

### Stories

#### **STORY 1.1:** Monorepo Setup with Nx

**Points:** 5 | **Priority:** Critical

**Acceptance Criteria:**

- Nx workspace initialized with Angular and Spring Boot apps
- Build and serve commands working for both apps
- Shared libraries structure created
- Git repository initialized with .gitignore

**Technical Details:**

```bash
# Initialize Nx workspace
npx create-nx-workspace@latest labelreader --preset=empty

# Add Angular application
cd labelreader
npm install -D @nx/angular
npx nx g @nx/angular:app frontend --routing --style=scss --standalone=false

# Create backend structure (manual)
mkdir -p apps/backend/src/main/java/com/labelreader
mkdir -p apps/backend/src/main/resources

# Add shared libraries
npx nx g @nx/js:library shared-models
```

**Dependencies:**

```json
// package.json (frontend dependencies)
{
  "@angular/animations": "^21.0.1",
  "@angular/common": "^21.0.1",
  "@angular/core": "^21.0.1",
  "@angular/forms": "^21.0.1",
  "@angular/material": "^21.0.1",
  "@angular/platform-browser": "^21.0.1",
  "@angular/router": "^21.0.1",
  "rxjs": "^7.8.1",
  "wavesurfer.js": "^7.7.0",
  "ngx-toastr": "^18.0.0"
}
```

```xml
<!-- pom.xml (backend dependencies) -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.4.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

#### **STORY 1.2:** Docker Configuration

**Points:** 8 | **Priority:** Critical

**Acceptance Criteria:**

- Dockerfile for Angular frontend (multi-stage build with Nginx)
- Dockerfile for Spring Boot backend
- Docker Compose orchestrating all services
- Environment variables properly configured
- All containers can communicate

**Technical Details:**

**Frontend Dockerfile:**

```dockerfile
# apps/frontend/Dockerfile
FROM node:22-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build:production

FROM nginx:1.25-alpine
COPY --from=builder /app/dist/apps/frontend /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Backend Dockerfile:**

```dockerfile
# apps/backend/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Docker Compose:**

```yaml
# docker/docker-compose.yml
version: "3.9"

services:
  mysql:
    image: mysql:latest
    container_name: labelreader-db
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: ${DB_NAME:-labelreader}
      MYSQL_USER: ${DB_USER:-labelreader_user}
      MYSQL_PASSWORD: ${DB_PASSWORD:-labelreader_pass}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - labelreader-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ../apps/backend
      dockerfile: Dockerfile
    container_name: labelreader-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${DB_NAME:-labelreader}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-labelreader_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-labelreader_pass}
      JWT_SECRET: ${JWT_SECRET:-your-secret-key-change-in-production}
      FILE_UPLOAD_PATH: /app/uploads
    ports:
      - "8080:8080"
    volumes:
      - backend_uploads:/app/uploads
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - labelreader-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ..
      dockerfile: apps/frontend/Dockerfile
    container_name: labelreader-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - labelreader-network
    environment:
      API_URL: http://backend:8080

volumes:
  mysql_data:
  backend_uploads:

networks:
  labelreader-network:
    driver: bridge
```

---

#### **STORY 1.3:** Database Schema Design

**Points:** 8 | **Priority:** Critical

**Acceptance Criteria:**

- MySQL schema created with all necessary tables
- Indexes optimized for common queries
- Foreign key constraints properly defined
- Migration scripts ready

**Database Schema:**

```sql
-- docker/init.sql

CREATE DATABASE IF NOT EXISTS labelreader CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE labelreader;

-- Users table (artists and label representatives)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    user_type ENUM('ARTIST', 'LABEL') NOT NULL,
    profile_image_url VARCHAR(500),
    bio TEXT,
    country VARCHAR(100),
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_user_type (user_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- Artist profiles
CREATE TABLE artist_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    artist_name VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    website_url VARCHAR(500),
    spotify_url VARCHAR(500),
    instagram_handle VARCHAR(100),
    twitter_handle VARCHAR(100),
    soundcloud_url VARCHAR(500),
    total_submissions INT DEFAULT 0,
    total_plays INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_genre (genre),
    INDEX idx_artist_name (artist_name)
) ENGINE=InnoDB;

-- Label profiles
CREATE TABLE label_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    label_name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    website_url VARCHAR(500),
    genres_interested JSON,
    country VARCHAR(100),
    total_reviews INT DEFAULT 0,
    total_signed INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_label_name (label_name),
    INDEX idx_country (country)
) ENGINE=InnoDB;

-- Music submissions
CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    sub_genre VARCHAR(100),
    bpm INT,
    key_signature VARCHAR(10),
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    duration_seconds INT,
    waveform_data JSON,
    description TEXT,
    lyrics TEXT,
    release_date DATE,
    is_published BOOLEAN DEFAULT FALSE,
    submission_status ENUM('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    play_count INT DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    total_ratings INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (artist_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_artist_id (artist_id),
    INDEX idx_genre (genre),
    INDEX idx_status (submission_status),
    INDEX idx_created_at (created_at),
    INDEX idx_average_rating (average_rating),
    FULLTEXT INDEX ft_title_artist (title, artist_name, description)
) ENGINE=InnoDB;

-- Ratings and reviews
CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_interested BOOLEAN DEFAULT FALSE,
    listened_duration_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_rating (submission_id, label_id),
    INDEX idx_submission_id (submission_id),
    INDEX idx_label_id (label_id),
    INDEX idx_rating (rating),
    INDEX idx_is_interested (is_interested)
) ENGINE=InnoDB;

-- Label interest/signing requests
CREATE TABLE signing_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    request_status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN') DEFAULT 'PENDING',
    message TEXT,
    contract_terms TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (artist_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_submission_id (submission_id),
    INDEX idx_label_id (label_id),
    INDEX idx_artist_id (artist_id),
    INDEX idx_status (request_status)
) ENGINE=InnoDB;

-- Refresh tokens for JWT
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB;

-- Notifications
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('NEW_RATING', 'SIGNING_REQUEST', 'REQUEST_RESPONSE', 'SYSTEM') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- Play history/analytics
CREATE TABLE play_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    ip_address VARCHAR(45),
    duration_played_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_submission_id (submission_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;
```

**JPA Entity Example (User):**

```java
// apps/backend/src/main/java/com/labelreader/entity/User.java
package com.labelreader.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 100)
    private String country;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public enum UserType {
        ARTIST, LABEL
    }
}
```

---

#### **STORY 1.4:** Authentication System (JWT)

**Points:** 13 | **Priority:** Critical

**Acceptance Criteria:**

- User registration for artists and labels
- Login with JWT token generation
- Refresh token mechanism
- Password encryption with BCrypt
- Protected routes/endpoints

**Backend Implementation:**

```java
// Security Configuration
package com.labelreader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/artist/**").hasAuthority("ARTIST")
                .requestMatchers("/api/label/**").hasAuthority("LABEL")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**Frontend Auth Service:**

```typescript
// apps/frontend/src/app/core/services/auth.service.ts
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, Observable } from "rxjs";
import { tap } from "rxjs/operators";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    userType: "ARTIST" | "LABEL";
  };
}

@Injectable({ providedIn: "root" })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const user = localStorage.getItem("currentUser");
    if (user) {
      this.currentUserSubject.next(JSON.parse(user));
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>("/api/auth/login", credentials).pipe(
      tap((response) => {
        localStorage.setItem("accessToken", response.accessToken);
        localStorage.setItem("refreshToken", response.refreshToken);
        localStorage.setItem("currentUser", JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("currentUser");
    this.currentUserSubject.next(null);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem("accessToken");
  }
}
```

---

## Phase 2: Core Features - Artist Portal (Sprint 3-4)

**Goal:** Build the artist-facing features including music upload, profile management, and submission tracking.

### Stories

#### **STORY 2.1:** Artist Registration & Profile

**Points:** 8 | **Priority:** High

**Acceptance Criteria:**

- Registration form with artist-specific fields
- Profile creation with social media links
- Avatar upload functionality
- Genre selection from predefined list

**API Endpoints:**

```
POST   /api/auth/register/artist
GET    /api/artist/profile
PUT    /api/artist/profile
POST   /api/artist/profile/avatar
```

---

#### **STORY 2.2:** Music Upload System

**Points:** 13 | **Priority:** Critical

**Acceptance Criteria:**

- File upload with progress indicator
- Support for MP3, WAV, FLAC formats
- Audio metadata extraction (duration, BPM, etc.)
- Waveform generation for visualization
- Max file size validation (50MB)

**Backend Controller:**

```java
@RestController
@RequestMapping("/api/artist/submissions")
public class SubmissionController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> uploadSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("genre") String genre,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        // Implementation
    }
}
```

**Frontend Upload Component:**

```typescript
// File upload with drag-and-drop
@Component({
  selector: "app-music-upload",
  template: `
    <div
      class="upload-zone"
      (drop)="onDrop($event)"
      (dragover)="onDragOver($event)"
    >
      <input
        type="file"
        #fileInput
        accept=".mp3,.wav,.flac"
        (change)="onFileSelected($event)"
      />
      <button (click)="fileInput.click()">Choose File</button>
      <div *ngIf="uploadProgress > 0">
        <mat-progress-bar [value]="uploadProgress"></mat-progress-bar>
      </div>
    </div>
  `,
})
export class MusicUploadComponent {
  uploadProgress = 0;

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    this.uploadFile(file);
  }

  uploadFile(file: File): void {
    const formData = new FormData();
    formData.append("file", file);
    // Add metadata
    this.http
      .post("/api/artist/submissions", formData, {
        reportProgress: true,
        observe: "events",
      })
      .subscribe((event) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.uploadProgress = Math.round((100 * event.loaded) / event.total!);
        }
      });
  }
}
```

---

#### **STORY 2.3:** Artist Dashboard

**Points:** 8 | **Priority:** High

**Acceptance Criteria:**

- List all submissions with status
- View play counts and ratings
- Filter by status (pending, under review, approved, rejected)
- Quick stats overview (total plays, average rating, signing requests)

**API Endpoints:**

```
GET    /api/artist/submissions?status={status}&page={page}&size={size}
GET    /api/artist/submissions/{id}
GET    /api/artist/stats
DELETE /api/artist/submissions/{id}
```

---

## Phase 3: Core Features - Label Portal (Sprint 5-6)

**Goal:** Build label-facing features for discovering, reviewing, and rating submissions.

### Stories

#### **STORY 3.1:** Label Registration & Profile

**Points:** 8 | **Priority:** High

**Acceptance Criteria:**

- Registration form with company details
- Genre preferences (multi-select)
- Company logo upload
- Label profile page

**API Endpoints:**

```
POST   /api/auth/register/label
GET    /api/label/profile
PUT    /api/label/profile
POST   /api/label/profile/logo
```

---

#### **STORY 3.2:** Music Discovery Feed

**Points:** 13 | **Priority:** Critical

**Acceptance Criteria:**

- Paginated list of submissions
- Filter by genre, date, rating
- Search by title, artist name
- Sort by newest, most played, highest rated
- Infinite scroll or pagination

**API Endpoints:**

```
GET /api/label/discover?genre={genre}&search={query}&sort={field}&page={page}
```

**Frontend Component:**

```typescript
@Component({
  selector: "app-discovery-feed",
  template: `
    <div class="discovery-container">
      <app-filter-sidebar (filterChange)="onFilterChange($event)">
      </app-filter-sidebar>

      <div class="submissions-grid">
        <app-submission-card
          *ngFor="let submission of submissions"
          [submission]="submission"
          (play)="onPlay($event)"
          (rate)="onRate($event)"
        >
        </app-submission-card>
      </div>

      <button *ngIf="hasMore" (click)="loadMore()" class="load-more-btn">
        Load More
      </button>
    </div>
  `,
})
export class DiscoveryFeedComponent implements OnInit {
  submissions: Submission[] = [];
  filters: any = {};
  page = 0;
  hasMore = true;

  ngOnInit(): void {
    this.loadSubmissions();
  }

  loadSubmissions(): void {
    this.submissionService
      .discover(this.filters, this.page)
      .subscribe((response) => {
        this.submissions.push(...response.content);
        this.hasMore = !response.last;
      });
  }
}
```

---

#### **STORY 3.3:** Audio Player with Waveform

**Points:** 13 | **Priority:** High

**Acceptance Criteria:**

- Embedded audio player in submission cards
- Waveform visualization using WaveSurfer.js
- Play/pause, seek, volume controls
- Track play duration for analytics

**Frontend Implementation:**

```typescript
import WaveSurfer from "wavesurfer.js";

@Component({
  selector: "app-audio-player",
  template: `
    <div class="player-container">
      <div #waveform class="waveform"></div>
      <div class="controls">
        <button (click)="togglePlay()">
          {{ isPlaying ? "Pause" : "Play" }}
        </button>
        <span
          >{{ currentTime | formatTime }} / {{ duration | formatTime }}</span
        >
      </div>
    </div>
  `,
})
export class AudioPlayerComponent implements AfterViewInit, OnDestroy {
  @ViewChild("waveform") waveformEl!: ElementRef;
  @Input() audioUrl!: string;
  @Input() submissionId!: number;

  private wavesurfer!: WaveSurfer;
  isPlaying = false;
  currentTime = 0;
  duration = 0;

  ngAfterViewInit(): void {
    this.wavesurfer = WaveSurfer.create({
      container: this.waveformEl.nativeElement,
      waveColor: "#4ecca3",
      progressColor: "#2d7a5f",
      height: 64,
      barWidth: 2,
      barGap: 1,
    });

    this.wavesurfer.load(this.audioUrl);

    this.wavesurfer.on("ready", () => {
      this.duration = this.wavesurfer.getDuration();
    });

    this.wavesurfer.on("audioprocess", () => {
      this.currentTime = this.wavesurfer.getCurrentTime();
    });

    this.wavesurfer.on("finish", () => {
      this.trackPlayCompletion();
    });
  }

  togglePlay(): void {
    this.wavesurfer.playPause();
    this.isPlaying = !this.isPlaying;
  }

  ngOnDestroy(): void {
    this.wavesurfer?.destroy();
  }

  private trackPlayCompletion(): void {
    this.http
      .post("/api/analytics/play", {
        submissionId: this.submissionId,
        durationPlayed: this.currentTime,
      })
      .subscribe();
  }
}
```

---

#### **STORY 3.4:** Rating & Review System

**Points:** 8 | **Priority:** Critical

**Acceptance Criteria:**

- 5-star rating system
- Optional text review
- Mark as "Interested" flag
- View own ratings history

**API Endpoints:**

```
POST   /api/label/ratings
PUT    /api/label/ratings/{id}
GET    /api/label/ratings?page={page}
GET    /api/label/submissions/{submissionId}/rating
```

**Backend Service:**

```java
@Service
public class RatingService {

    @Transactional
    public RatingResponse rateSubmission(Long submissionId, Long labelId, RatingRequest request) {
        // Create or update rating
        Rating rating = ratingRepository
            .findBySubmissionIdAndLabelId(submissionId, labelId)
            .orElse(new Rating());

        rating.setSubmissionId(submissionId);
        rating.setLabelId(labelId);
        rating.setRating(request.getRating());
        rating.setReviewText(request.getReviewText());
        rating.setIsInterested(request.getIsInterested());

        ratingRepository.save(rating);

        // Update submission average rating
        updateSubmissionAverageRating(submissionId);

        // Notify artist
        notificationService.notifyNewRating(submissionId, labelId);

        return mapToResponse(rating);
    }

    private void updateSubmissionAverageRating(Long submissionId) {
        Double avgRating = ratingRepository.calculateAverageRating(submissionId);
        Integer totalRatings = ratingRepository.countBySubmissionId(submissionId);

        submissionRepository.updateRatingStats(submissionId, avgRating, totalRatings);
    }
}
```

---

#### **STORY 3.5:** Signing Request System

**Points:** 8 | **Priority:** High

**Acceptance Criteria:**

- Send signing request to artist
- Include custom message and contract terms
- Track request status (pending/accepted/declined)
- Withdraw request option

**API Endpoints:**

```
POST   /api/label/signing-requests
GET    /api/label/signing-requests?status={status}
PUT    /api/label/signing-requests/{id}/withdraw
```

---

## Phase 4: Advanced Features & Polish (Sprint 7-8)

**Goal:** Add notifications, analytics, search improvements, and UI polish.

### Stories

#### **STORY 4.1:** Real-time Notifications

**Points:** 13 | **Priority:** Medium

**Acceptance Criteria:**

- Email notifications for key events
- In-app notification center
- Mark as read/unread
- Notification badges

**Technologies:**

- Spring Boot: JavaMailSender
- Frontend: Angular notification service

**API Endpoints:**

```
GET    /api/notifications?unread={boolean}
PUT    /api/notifications/{id}/read
PUT    /api/notifications/read-all
DELETE /api/notifications/{id}
```

---

#### **STORY 4.2:** Advanced Search & Filters

**Points:** 8 | **Priority:** Medium

**Acceptance Criteria:**

- Full-text search on title, artist, description
- Multiple genre filters
- BPM range filter
- Date range filter
- Saved search preferences

**Backend - MySQL Full-Text Search:**

```java
@Query(value = "SELECT * FROM submissions WHERE " +
       "MATCH(title, artist_name, description) AGAINST (?1 IN NATURAL LANGUAGE MODE) " +
       "AND (?2 IS NULL OR genre = ?2) " +
       "AND (?3 IS NULL OR bpm BETWEEN ?3 AND ?4)",
       nativeQuery = true)
List<Submission> searchSubmissions(String query, String genre, Integer minBpm, Integer maxBpm);
```

---

#### **STORY 4.3:** Analytics Dashboard

**Points:** 13 | **Priority:** Medium

**Acceptance Criteria:**

- Play count charts (daily/weekly/monthly)
- Geographic distribution of listeners
- Top-rated submissions
- Engagement metrics for labels
- Export to CSV

**Technologies:**

- Frontend: Chart.js or ng2-charts
- Backend: Aggregate queries with Spring Data JPA

---

#### **STORY 4.4:** UI/UX Implementation (LabelRadar Theme)

**Points:** 13 | **Priority:** High

**Acceptance Criteria:**

- Implement dark teal/navy theme
- Green accent buttons throughout
- Responsive design (mobile, tablet, desktop)
- Smooth animations and transitions
- Loading states and error handling

**Angular Material Theme:**

```scss
// apps/frontend/src/styles.scss
@use "@angular/material" as mat;

$primary-palette: (
  50: #e8f5f1,
  100: #c5e6dc,
  200: #9fd6c5,
  300: #79c5ae,
  400: #5cb89c,
  500: #4ecca3,
  600: #3ea589,
  700: #2d7a5f,
  800: #1f6048,
  900: #124531,
  contrast: (
    50: rgba(black, 0.87),
    100: rgba(black, 0.87),
    200: rgba(black, 0.87),
    300: white,
    400: white,
    500: white,
    600: white,
    700: white,
    800: white,
    900: white,
  ),
);

$accent-palette: (
  50: #e3f2f8,
  100: #b9deed,
  200: #8bc9e1,
  300: #5db3d5,
  400: #3aa3cb,
  500: #1a2e3d,
  // ... define full palette
);

$primary: mat.define-palette($primary-palette, 500);
$accent: mat.define-palette($accent-palette, 500);
$warn: mat.define-palette(mat.$red-palette);

$theme: mat.define-dark-theme(
  (
    color: (
      primary: $primary,
      accent: $accent,
      warn: $warn,
    ),
  )
);

@include mat.all-component-themes($theme);

body {
  background-color: #1a2e3d;
  color: #ffffff;
  font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
```

---

## Phase 5: Testing & Deployment (Sprint 9)

### Stories

#### **STORY 5.1:** Unit & Integration Tests

**Points:** 13 | **Priority:** High

**Backend Testing:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubmissionService submissionService;

    @Test
    @WithMockUser(authorities = "ARTIST")
    void shouldUploadSubmission() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp3", "audio/mpeg", "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/artist/submissions")
                .file(file)
                .param("title", "Test Song"))
            .andExpect(status().isCreated());
    }
}
```

**Frontend Testing:**

```typescript
describe("AuthService", () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it("should login successfully", () => {
    const mockResponse: AuthResponse = {
      accessToken: "token",
      refreshToken: "refresh",
      user: {
        id: 1,
        email: "test@test.com",
        firstName: "Test",
        lastName: "User",
        userType: "ARTIST",
      },
    };

    service
      .login({ email: "test@test.com", password: "password" })
      .subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(localStorage.getItem("accessToken")).toBe("token");
      });

    const req = httpMock.expectOne("/api/auth/login");
    expect(req.request.method).toBe("POST");
    req.flush(mockResponse);
  });
});
```

---

#### **STORY 5.2:** Production Deployment Setup

**Points:** 13 | **Priority:** Critical

**Acceptance Criteria:**

- Environment-specific configurations
- Production Docker Compose
- SSL/TLS certificates
- Database backup strategy
- CI/CD pipeline (GitHub Actions example)

**GitHub Actions CI/CD:**

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "22"

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Build Frontend
        run: |
          npm ci
          npm run build:production

      - name: Build Backend
        run: |
          cd apps/backend
          mvn clean package -DskipTests

      - name: Build Docker Images
        run: |
          docker-compose -f docker/docker-compose.prod.yml build

      - name: Deploy to Server
        run: |
          # Deploy logic (SSH, Docker registry push, etc.)
```

**Production Docker Compose:**

```yaml
# docker/docker-compose.prod.yml
version: "3.9"

services:
  mysql:
    image: mysql:latest
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - labelreader-network

  backend:
    image: labelreader-backend:latest
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - mysql
    networks:
      - labelreader-network

  frontend:
    image: labelreader-frontend:latest
    restart: always
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
    networks:
      - labelreader-network

volumes:
  mysql_data:

networks:
  labelreader-network:
    driver: bridge
```

---

## Development Commands

### Setup & Installation

```bash
# Clone repository
git clone <repository-url>
cd labelreader

# Install dependencies
npm install

# Start development environment
docker-compose -f docker/docker-compose.dev.yml up --build

# Run frontend only (after Docker setup)
nx serve frontend

# Run backend only (after Docker setup)
cd apps/backend
mvn spring-boot:run

# Run tests
nx test frontend
cd apps/backend && mvn test
```

### Docker Commands

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Rebuild specific service
docker-compose build backend

# Access MySQL
docker exec -it labelreader-db mysql -u labelreader_user -p
```

---

## API Documentation

### Authentication Endpoints

| Method | Endpoint                    | Description          | Auth Required |
| ------ | --------------------------- | -------------------- | ------------- |
| POST   | `/api/auth/register/artist` | Register new artist  | No            |
| POST   | `/api/auth/register/label`  | Register new label   | No            |
| POST   | `/api/auth/login`           | Login user           | No            |
| POST   | `/api/auth/refresh`         | Refresh access token | No            |
| POST   | `/api/auth/logout`          | Logout user          | Yes           |

### Artist Endpoints

| Method | Endpoint                            | Description                | Auth Required |
| ------ | ----------------------------------- | -------------------------- | ------------- |
| GET    | `/api/artist/profile`               | Get artist profile         | Artist        |
| PUT    | `/api/artist/profile`               | Update artist profile      | Artist        |
| POST   | `/api/artist/submissions`           | Upload new submission      | Artist        |
| GET    | `/api/artist/submissions`           | List all submissions       | Artist        |
| GET    | `/api/artist/submissions/{id}`      | Get submission details     | Artist        |
| DELETE | `/api/artist/submissions/{id}`      | Delete submission          | Artist        |
| GET    | `/api/artist/stats`                 | Get artist statistics      | Artist        |
| GET    | `/api/artist/signing-requests`      | List signing requests      | Artist        |
| PUT    | `/api/artist/signing-requests/{id}` | Respond to signing request | Artist        |

### Label Endpoints

| Method | Endpoint                                    | Description            | Auth Required |
| ------ | ------------------------------------------- | ---------------------- | ------------- |
| GET    | `/api/label/profile`                        | Get label profile      | Label         |
| PUT    | `/api/label/profile`                        | Update label profile   | Label         |
| GET    | `/api/label/discover`                       | Discover submissions   | Label         |
| GET    | `/api/label/submissions/{id}`               | Get submission details | Label         |
| POST   | `/api/label/ratings`                        | Rate a submission      | Label         |
| PUT    | `/api/label/ratings/{id}`                   | Update rating          | Label         |
| GET    | `/api/label/ratings`                        | List label's ratings   | Label         |
| POST   | `/api/label/signing-requests`               | Send signing request   | Label         |
| GET    | `/api/label/signing-requests`               | List signing requests  | Label         |
| PUT    | `/api/label/signing-requests/{id}/withdraw` | Withdraw request       | Label         |

---

## Security Considerations

1. **File Upload Security:**

   - Validate file types (magic number checking)
   - Scan for malware
   - Limit file size (50MB)
   - Generate unique filenames to prevent overwriting

2. **JWT Security:**

   - Use strong secret key (256-bit minimum)
   - Short access token expiry (15 minutes)
   - Implement token rotation
   - Store refresh tokens securely in database

3. **SQL Injection Prevention:**

   - Use JPA parameterized queries
   - Validate all user inputs
   - Escape special characters

4. **CORS Configuration:**

   ```java
   @Configuration
   public class CorsConfig {
       @Bean
       public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {
               @Override
               public void addCorsMappings(CorsRegistry registry) {
                   registry.addMapping("/api/**")
                       .allowedOrigins("http://localhost:4200", "https://yourdomain.com")
                       .allowedMethods("GET", "POST", "PUT", "DELETE")
                       .allowedHeaders("*")
                       .allowCredentials(true);
               }
           };
       }
   }
   ```

5. **Rate Limiting:**
   - Implement request rate limiting (e.g., Bucket4j)
   - Limit upload frequency
   - Protect against brute force attacks

---

## Performance Optimization

1. **Database Indexing:**

   - Already included in schema (see indexes in SQL)
   - Monitor slow queries and add indexes as needed

2. **Caching:**

   - Redis for frequently accessed data
   - Cache submission metadata
   - Cache user profiles

3. **File Delivery:**

   - Use CDN for audio files in production
   - Implement HTTP range requests for streaming
   - Generate lower-quality previews for browsing

4. **Frontend Optimization:**
   - Lazy loading for routes
   - Virtual scrolling for large lists
   - Image/audio lazy loading
   - Bundle size optimization

---

## Monitoring & Logging

**Backend Logging (Logback):**

```xml
<!-- apps/backend/src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

**Metrics:**

- Use Spring Boot Actuator
- Expose `/actuator/health`, `/actuator/metrics`
- Integrate with Prometheus/Grafana for visualization

---

## Success Metrics

### Technical KPIs

- **Uptime:** 99.5%+
- **API Response Time:** < 500ms (p95)
- **File Upload Success Rate:** > 98%
- **Database Query Time:** < 100ms (p95)

### Business KPIs

- **Daily Active Users (DAU)**
- **Monthly Active Users (MAU)**
- **Total Submissions**
- **Total Ratings**
- **Signing Request Conversion Rate**

---

## Future Enhancements (Post-Launch)

1. **Social Features:**

   - Artist following
   - Label following
   - Comments on submissions
   - Share to social media

2. **Advanced Analytics:**

   - A/B testing for UI elements
   - Recommendation engine (ML-based)
   - Submission quality scoring

3. **Mobile Apps:**

   - iOS app (Swift/SwiftUI)
   - Android app (Kotlin)
   - Share codebase with Ionic/Capacitor

4. **Payment Integration:**

   - Premium artist accounts
   - Featured submissions
   - Label subscription tiers

5. **AI Features:**
   - Auto-tagging (genre detection)
   - Similar track recommendations
   - Mastering quality analysis

---

## Summary

This implementation plan provides a comprehensive roadmap for building a professional music platform in **5 phases over 9 sprints**. The architecture uses a modern monorepo structure with Angular 21, Spring Boot 3.4, and MySQL, all containerized with Docker for easy deployment.

**Key deliverables:**

- 20+ user stories with detailed acceptance criteria
- Complete database schema with 9 tables
- 30+ API endpoints documented
- Docker setup for development and production
- Security, performance, and monitoring strategies
- Testing approach for frontend and backend

The plan is ready for your development team to begin implementation immediately.
