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
  "email": "user@example.com",
  "password": "YOUR_PASSWORD"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
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

## Development vs Production

### Development (`app.jwt.secret` default value)
- Use provided test secret
- Resume files stored locally
- CORS allows http://localhost:5173

### Production
1. Change `app.jwt.secret` to generated strong key
2. Use environment variables for secrets
3. Enable HTTPS
4. Consider moving uploads to cloud storage (AWS S3, etc.)
5. Set up proper logging & monitoring
6. Use HTTP-only, Secure cookies for JWT (not localStorage)

## API Testing

### Using cURL

```bash
# 1. Login with an existing account
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"YOUR_PASSWORD"}' \
  | jq -r '.token')

# 2. Get candidates (with token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/candidates

# 3. Download resume
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

## Admin registration verification and approval

`POST /api/auth/signup` creates an Admin account with `email_verified=false` and
`admin_approval_status=PENDING`. It returns an email/role/message response, never a JWT.
After the database transaction commits, two independent asynchronous email jobs run:

- The registered email receives a one-use email verification link.
- The configured approver receives approve/reject links. Opening a link shows a confirmation page; submitting that page records the decision.

Configure these properties (environment overrides are supported):

```properties
app.registration.base-url=${APP_REGISTRATION_BASE_URL:http://localhost:8080}
app.registration.approver-email=${APP_REGISTRATION_APPROVER_EMAIL:chenchuthestar@gmail.com}
app.registration.token-expiration-hours=${APP_REGISTRATION_TOKEN_EXPIRATION_HOURS:48}
```

Set the base URL to the externally reachable HTTPS API origin in deployment, without `/api`.
Both emails use the existing `spring.mail.*` SMTP configuration and Thymeleaf templates
loaded from the classpath, including packaged JAR deployments.

Login checks the password first, then requires both email verification and `APPROVED`
status for Admin accounts. Rejected or pending admins cannot log in. Existing JWTs are
also checked against the current database status. Other roles use the existing `active`
field and newly registered non-admin accounts are marked `approved`.

Persistence includes `email_verified`, `email_verified_at`, `admin_approval_status`,
`admin_reviewed_at`, and `admin_reviewed_by` on `users`, plus the `registration_tokens`
table. The configured `spring.jpa.hibernate.ddl-auto=update` adds these on startup;
existing accounts default to unverified/pending rather than gaining admin access.
For environments using managed migrations, add the equivalent columns/table before deploying.
Tokens are random 256-bit values; only SHA-256 hashes are stored. Purpose, expiry, and
consumption are checked, with database locks preventing simultaneous token reuse.
The old user-ID/email-only approval/rejection endpoints have been removed.

SMTP delivery is retried three times and failures are logged without tokens. Jobs run
in memory and are not a durable queue; process interruption or persistent SMTP failure
requires support intervention. Expired links also require support; automatic resend
is not implemented.

Run `mvn test` for login status combinations, signup, independent verification/review,
expired/wrong-purpose/reused tokens, and approval preview checks. Tests mock mail delivery.

## Job seeker registration confirmation

After candidate registration commits, an asynchronous confirmation email goes to the
submitted candidate email using the configured `spring.mail.*` settings. It includes the
saved registration ID, personal/reference details, resume filename, education, and
experience details for experienced candidates. Thymeleaf escapes submitted values and
loads the template from the classpath in packaged JAR deployments.

Email delivery is retried three times; persistent failure is logged by registration ID.
A failed delivery does not undo registration. Like admin emails, these jobs run in memory
and require support intervention if interrupted or if SMTP remains unavailable.

## Candidate active/inactive status

Candidates default to active, including existing rows through the `active` column's
`boolean default true` schema definition. Admins can change the status at the top of the
candidate details page. `PATCH /api/candidates/{id}/status` accepts `{ "active": false }`
or `{ "active": true }`, persists the change, and returns the updated candidate.
The endpoint requires the Admin role loaded from the database by the JWT filter.
`GET /api/auth/me` returns the current email and role for displaying admin-only controls.
Inactive candidates remain visible and their details/resume remain accessible; status
changes do not delete registrations or send another registration confirmation email.

## Matching job opening emails

Creating an OPEN job or changing a CLOSED/ON_HOLD job to OPEN publishes a notification
event. After commit, matching active candidates receive individual emails. Both job category
(IT/NON_IT normalized to IT_Job/Non_IT_Job) and Fresher/Experienced candidate type must match.
Ordinary edits to an already-open job do not resend; reopening a job does. Closed, expired,
and unclassified jobs are skipped. Candidate email addresses are deduplicated per event.

The email includes the job details and a WhatsApp contact button with a URL-encoded message
identifying the opening and registration. Configure `app.jobs.whatsapp-number` or
`APP_JOBS_WHATSAPP_NUMBER` with the full international number (default `919618254711`).
WhatsApp opens with a prefilled message; the candidate reviews and sends it themselves.
Delivery is asynchronous, retried three times per recipient, and persistent failures are
logged without stopping other recipients. These in-memory jobs are not a durable mail queue.
