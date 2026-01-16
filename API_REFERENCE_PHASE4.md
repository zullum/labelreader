# Phase 4 API Reference

Quick reference for all new API endpoints added in Phase 4.

---

## 🔔 Notification API

### Get User Notifications
```http
GET /api/notifications?page=0&size=20&unreadOnly=false
Authorization: Bearer <token>
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "type": "NEW_RATING",
      "title": "New Rating Received",
      "message": "Label XYZ rated your track 'Song Title'",
      "linkUrl": "/artist/submissions/123",
      "isRead": false,
      "createdAt": "2025-12-01T10:00:00"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

### Get Unread Count
```http
GET /api/notifications/unread-count
Authorization: Bearer <token>
```

**Response:**
```json
12
```

### Mark as Read
```http
PUT /api/notifications/{id}/read
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": 1,
  "type": "NEW_RATING",
  "title": "New Rating Received",
  "message": "Label XYZ rated your track",
  "linkUrl": "/artist/submissions/123",
  "isRead": true,
  "createdAt": "2025-12-01T10:00:00"
}
```

### Mark All as Read
```http
PUT /api/notifications/read-all
Authorization: Bearer <token>
```

**Response:** `200 OK`

### Delete Notification
```http
DELETE /api/notifications/{id}
Authorization: Bearer <token>
```

**Response:** `200 OK`

---

## 🔍 Search API

### Full-Text Search
```http
GET /api/search?query=rock&genre=Rock&minBpm=120&maxBpm=140&sortBy=averageRating&sortDirection=DESC&page=0&size=20
Authorization: Bearer <token>
```

**Query Parameters:**
- `query` (optional) - Full-text search query
- `genre` (optional) - Filter by single genre
- `minBpm` (optional) - Minimum BPM
- `maxBpm` (optional) - Maximum BPM
- `startDate` (optional) - Filter from date (ISO 8601)
- `endDate` (optional) - Filter to date (ISO 8601)
- `sortBy` (optional) - Sort field (default: `createdAt`)
  - Options: `createdAt`, `playCount`, `averageRating`, `title`, `artistName`
- `sortDirection` (optional) - Sort direction (default: `DESC`)
  - Options: `ASC`, `DESC`
- `page` (optional) - Page number (default: 0)
- `size` (optional) - Page size (default: 20)

**Response:**
```json
{
  "content": [
    {
      "id": 123,
      "title": "Rock Anthem",
      "artistName": "John Doe",
      "genre": "Rock",
      "bpm": 130,
      "averageRating": 4.5,
      "playCount": 1250,
      "createdAt": "2025-11-15T10:00:00"
    }
  ],
  "totalElements": 145,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

### Advanced Filter
```http
GET /api/search/filter?genres=Rock,Metal&minBpm=120&maxBpm=160&minRating=4.0&sortBy=playCount&sortDirection=DESC
Authorization: Bearer <token>
```

**Query Parameters:**
- `genres` (optional) - Multiple genres (comma-separated)
- `minBpm` (optional) - Minimum BPM
- `maxBpm` (optional) - Maximum BPM
- `minRating` (optional) - Minimum average rating
- `sortBy` (optional) - Sort field
- `sortDirection` (optional) - Sort direction
- `page` (optional) - Page number
- `size` (optional) - Page size

**Response:** Same format as Full-Text Search

---

## 📊 Analytics API

### Get Artist Analytics
```http
GET /api/analytics/artist?days=30
Authorization: Bearer <token>
```

**Query Parameters:**
- `days` (optional) - Number of days for time series data (default: 30)

**Response:**
```json
{
  "totalSubmissions": 15,
  "totalPlays": 5420,
  "averageRating": 4.2,
  "totalRatings": 234,
  "signingRequests": 3,
  "playsByDate": [
    {
      "date": "2025-11-01",
      "playCount": 120
    },
    {
      "date": "2025-11-02",
      "playCount": 145
    }
  ],
  "topSubmissions": [
    {
      "id": 123,
      "title": "Hit Song",
      "artistName": "John Doe",
      "playCount": 1250,
      "averageRating": 4.8,
      "totalRatings": 45
    }
  ]
}
```

### Get Specific Artist Analytics
```http
GET /api/analytics/artist/{artistId}?days=30
Authorization: Bearer <token>
```

**Response:** Same format as Get Artist Analytics

### Get Label Analytics
```http
GET /api/analytics/label
Authorization: Bearer <token>
```

**Response:**
```json
{
  "totalReviews": 342,
  "totalSigningRequests": 12,
  "averageRatingGiven": 3.8,
  "reviewsByGenre": {
    "Rock": 120,
    "Electronic": 85,
    "Hip-Hop": 67
  },
  "recentlyReviewed": [
    {
      "id": 456,
      "title": "New Track",
      "artistName": "Jane Smith",
      "playCount": 450,
      "averageRating": 4.2,
      "totalRatings": 12
    }
  ]
}
```

### Get Specific Label Analytics
```http
GET /api/analytics/label/{labelId}
Authorization: Bearer <token>
```

**Response:** Same format as Get Label Analytics

### Get Platform Analytics
```http
GET /api/analytics/platform
```

**Response:**
```json
{
  "totalSubmissions": 5420,
  "totalArtists": 1234,
  "totalLabels": 89,
  "totalPlays": 1250000,
  "genreDistribution": [
    {
      "genre": "Rock",
      "count": 1250,
      "percentage": 23.1
    },
    {
      "genre": "Electronic",
      "count": 980,
      "percentage": 18.1
    }
  ],
  "topRatedSubmissions": [
    {
      "id": 789,
      "title": "Masterpiece",
      "artistName": "Top Artist",
      "playCount": 5000,
      "averageRating": 4.9,
      "totalRatings": 120
    }
  ],
  "mostPlayedSubmissions": [
    {
      "id": 101,
      "title": "Viral Hit",
      "artistName": "Famous Artist",
      "playCount": 15000,
      "averageRating": 4.3,
      "totalRatings": 345
    }
  ]
}
```

---

## 🔐 Authentication

All endpoints (except Platform Analytics) require JWT authentication:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

Get token from login endpoint:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

---

## 📝 Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-12-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid parameter value"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-12-01T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-12-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Notification not found"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-12-01T10:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## 🧪 Testing Examples

### cURL Examples

**Get Notifications:**
```bash
curl -X GET "http://localhost:8080/api/notifications?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Search Submissions:**
```bash
curl -X GET "http://localhost:8080/api/search?query=rock&minBpm=120&maxBpm=140" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Get Artist Analytics:**
```bash
curl -X GET "http://localhost:8080/api/analytics/artist?days=30" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Mark Notification as Read:**
```bash
curl -X PUT "http://localhost:8080/api/notifications/1/read" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### JavaScript/TypeScript Examples

**Fetch Notifications:**
```typescript
const response = await fetch('/api/notifications?page=0&size=20', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const data = await response.json();
```

**Search with Filters:**
```typescript
const params = new URLSearchParams({
  query: 'rock',
  genre: 'Rock',
  minBpm: '120',
  maxBpm: '140',
  sortBy: 'averageRating',
  sortDirection: 'DESC'
});

const response = await fetch(`/api/search?${params}`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const results = await response.json();
```

**Get Analytics:**
```typescript
const response = await fetch('/api/analytics/artist?days=30', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const analytics = await response.json();
```

---

## 📦 Response Pagination

All paginated endpoints return:
- `content`: Array of items
- `totalElements`: Total number of items
- `totalPages`: Total number of pages
- `number`: Current page number (0-indexed)
- `size`: Items per page
- `first`: Boolean indicating if first page
- `last`: Boolean indicating if last page

---

## 🎯 Rate Limiting

Recommended rate limits (not yet implemented):
- Search: 60 requests per minute
- Analytics: 30 requests per minute
- Notifications: 100 requests per minute

---

## 📚 Additional Resources

- Full API Documentation: See `implementation_plan.md`
- Authentication Guide: See Phase 1 documentation
- Database Schema: See `docker/init.sql`
