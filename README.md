# LabelReader - Music Platform

A comprehensive music submission and review platform connecting independent artists with record labels.

## Technology Stack

- **Frontend:** Angular 21
- **Backend:** Spring Boot 3.4.0 (Java 21)
- **Database:** MySQL 8.2+
- **Containerization:** Docker & Docker Compose
- **Monorepo:** Nx

## Project Structure

```
labelreader/
├── apps/
│   ├── frontend/          # Angular application (to be created)
│   └── backend/           # Spring Boot application
│       ├── src/
│       ├── Dockerfile
│       └── pom.xml
├── docker/
│   ├── docker-compose.yml
│   └── init.sql
├── nx.json
├── package.json
└── README.md
```

## Prerequisites

- **Node.js:** 22 LTS
- **Java:** OpenJDK 21
- **Maven:** 3.9+
- **Docker:** 24+
- **Docker Compose:** 2.23+

## Getting Started

### 1. Install Dependencies

```bash
npm install
```

### 2. Start with Docker (Recommended)

Start all services (MySQL, Backend, Frontend):

```bash
cd docker
docker-compose up --build
```

Services will be available at:

- **Frontend:** http://localhost:80
- **Backend API:** http://localhost:8080
- **MySQL:** localhost:3306

### 3. Development Mode (Without Docker)

#### Start MySQL

```bash
cd docker
docker-compose up mysql
```

#### Run Backend

```bash
cd apps/backend
mvn spring-boot:run
```

#### Run Frontend (once created)

```bash
npm start
# or
nx serve frontend
```

## Environment Variables

Create a `.env` file in the `docker/` directory:

```env
DB_ROOT_PASSWORD=rootpassword
DB_NAME=labelreader
DB_USER=labelreader_user
DB_PASSWORD=labelreader_pass
JWT_SECRET=your-secret-key-change-in-production-must-be-at-least-256-bits
```

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user (artist or label)
- `POST /api/auth/login` - Login user

### Artist Endpoints (Coming Soon)

- `GET /api/artist/profile` - Get artist profile
- `POST /api/artist/submissions` - Upload music submission

### Label Endpoints (Coming Soon)

- `GET /api/label/discover` - Discover music submissions
- `POST /api/label/ratings` - Rate a submission

## Database Schema

The database includes 9 tables:

- `users` - User accounts (artists and labels)
- `artist_profiles` - Artist-specific information
- `label_profiles` - Label-specific information
- `submissions` - Music submissions
- `ratings` - Label ratings and reviews
- `signing_requests` - Label signing requests
- `refresh_tokens` - JWT refresh tokens
- `notifications` - User notifications
- `play_history` - Analytics tracking

## Development Status

### ✅ Completed (Phase 1)

- Monorepo setup with Nx
- Spring Boot backend structure
- MySQL database schema
- Docker configuration
- JWT authentication system
- User registration and login

### 🚧 In Progress

- Angular frontend setup
- Frontend authentication UI

### 📋 Planned

- Artist portal (music upload, dashboard)
- Label portal (discovery feed, ratings)
- Audio player with waveform
- Notifications system
- Analytics dashboard

## Testing

### Backend Tests

```bash
cd apps/backend
mvn test
```

### Frontend Tests (once created)

```bash
nx test frontend
```

## Docker Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f backend

# Rebuild specific service
docker-compose build backend

# Access MySQL
docker exec -it labelreader-db mysql -u labelreader_user -p
```

## Contributing

This project follows a 5-phase implementation plan:

1. Foundation & Infrastructure ✅
2. Core Features - Artist Portal
3. Core Features - Label Portal
4. Advanced Features & Polish
5. Testing & Deployment

See `implementation_plan.md` for detailed specifications.

## License

MIT
