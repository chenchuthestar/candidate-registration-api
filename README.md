# Candidate Registration Backend (Spring Boot 4.1 + MySQL + JWT)

REST API for the Candidate Registration frontend with JWT authentication,
resume file handling, and MySQL persistence.

## Features

- ✅ **JWT Authentication** — Login/token-based access to APIs
- ✅ Spring Security with bcrypt password hashing
- ✅ Protected endpoints (require valid JWT token)
- ✅ Resume file upload & download (PDF/DOC/DOCX)
- ✅ MySQL database with Hibernate auto-schema creation
- ✅ Global exception handling & validation
- ✅ CORS configured for Vite dev server

## Requirements

- Java 17+
- Maven 3.9+
- MySQL 8.x
- Node 18+ (for frontend only)

## 1. Configure MySQL

Create database (optional, Hibernate will auto-create):

```sql
CREATE DATABASE candidate_registration;
```

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

## 2. Configure JWT Secret (IMPORTANT)

**Default secret is for testing only.** For production, generate a secure key:

```bash
openssl rand -base64 32
```

Edit `src/main/resources/application.properties`:

```properties
app.jwt.secret=YOUR_GENERATED_SECRET_HERE
```

## 3. Run

```bash
mvn spring-boot:run
```

On first run:
- Database schema auto-creates
- `uploads/resumes/` folder is created
- App starts on `http://localhost:8080`

## API Endpoints

### Authentication (Public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | Login with email/password → returns JWT token |
| POST | `/api/auth/create-test-user` | Create a test user (development only) |

### Candidates (Protected - require JWT token)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/candidates` | Register new candidate with resume (multipart) |
| GET | `/api/candidates` | List all candidates |
| GET | `/api/candidates/{id}` | Get one candidate |
| GET | `/api/candidates/{id}/resume` | Download candidate's resume |
| DELETE | `/api/candidates/{id}` | Delete candidate (also deletes resume file) |

## Login Request/Response

### POST /api/auth/login

**Request:**
```json
{
  "email": "test@example.com",
  "password": "Test@123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "email": "test@example.com",
  "message": "Login successful"
}
```

**Response (400):**
```json
{
  "timestamp": "2026-07-25T...",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email or password"
}
```

## Protected Endpoints

All `/api/candidates/**` endpoints require `Authorization` header:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
```

If token is missing or invalid:
- Returns **HTTP 401 Unauthorized**
- Frontend automatically redirects to login

## Test User Creation

```bash
curl -X POST "http://localhost:8080/api/auth/create-test-user?email=test@example.com&password=Test@123"
```

Or use the frontend login page "Create Test User" button.

**Credentials:**
- Email: `test@example.com`
- Password: `Test@123`

## Database Schema

### users table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at BIGINT NOT NULL
);
```

### candidates table
```sql
CREATE TABLE candidates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL,
  mobile VARCHAR(10) NOT NULL,
  candidate_type VARCHAR(20) NOT NULL,
  resume_file_name VARCHAR(255),
  resume_stored_path VARCHAR(255) UNIQUE
);
```

### education_details table
```sql
CREATE TABLE education_details (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qualification VARCHAR(30) NOT NULL,
  institution_name VARCHAR(150) NOT NULL,
  board_or_university VARCHAR(150) NOT NULL,
  year_of_passing VARCHAR(4) NOT NULL,
  score VARCHAR(20) NOT NULL,
  candidate_id BIGINT NOT NULL FOREIGN KEY REFERENCES candidates(id)
);
```

### experience_details table
```sql
CREATE TABLE experience_details (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_name VARCHAR(150) NOT NULL,
  designation VARCHAR(150) NOT NULL,
  from_date VARCHAR(10) NOT NULL,
  to_date VARCHAR(10),
  currently_working BOOLEAN NOT NULL DEFAULT false,
  candidate_id BIGINT NOT NULL FOREIGN KEY REFERENCES candidates(id)
);
```

## File Storage

Resume files stored at: `uploads/resumes/[uuid].pdf`

- **Original name:** Stored in DB (`resume_file_name`)
- **Stored name:** UUID on disk (security)
- **Cleanup:** Auto-deleted when candidate is deleted

## JWT Token Details

- **Algorithm:** HS512 (HMAC SHA-512)
- **Expiration:** 24 hours (86,400,000 ms)
- **Subject:** User email
- **Issued At:** Timestamp
- **Claims:** Email, expiration, issued time

## Security

✅ Passwords hashed with bcrypt  
✅ JWT validated on every protected request  
✅ Token expiration enforced  
✅ CORS restricted to frontend origin  
✅ File upload validated (extension + size)  
⚠️ Change `app.jwt.secret` in production  
⚠️ Use HTTPS in production  

## Troubleshooting

### "Connection refused" on startup
→ MySQL not running. Start MySQL service.

### "Users table doesn't exist"
→ Hibernate didn't create schema. Check `spring.jpa.hibernate.ddl-auto=update` and MySQL permissions.

### "Invalid token" error
→ Token expired (24-hour default) or secret changed. Re-login.

### Resume file not downloaded
→ Ensure token is in `Authorization: Bearer {token}` header.
→ File may be deleted. Check `uploads/resumes/` folder.

### "Test user already exists"
→ User was already created. Use test@example.com / Test@123 or delete and recreate.

## Development vs Production

### Development (`app.jwt.secret` default value)
- Use provided test secret
- Create test users via endpoint
- Resume files stored locally
- CORS allows http://localhost:5173

### Production
1. Change `app.jwt.secret` to generated strong key
2. Disable `/api/auth/create-test-user` endpoint (remove or add authentication)
3. Use environment variables for secrets
4. Enable HTTPS
5. Consider moving uploads to cloud storage (AWS S3, etc.)
6. Set up proper logging & monitoring
7. Use HTTP-only, Secure cookies for JWT (not localStorage)

## API Testing

### Using cURL

```bash
# 1. Create test user
curl -X POST "http://localhost:8080/api/auth/create-test-user?email=test@example.com&password=Test@123"

# 2. Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@123"}' \
  | jq -r '.token')

# 3. Get candidates (with token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/candidates

# 4. Download resume
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/candidates/1/resume \
  -o resume.pdf
```

### Using Postman

1. Login at `POST /api/auth/login` → copy token
2. Add to **Authorization** tab → Type: Bearer Token → paste token
3. Use protected endpoints

---

See frontend README for complete authentication flow.
