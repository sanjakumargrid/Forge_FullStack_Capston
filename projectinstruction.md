# Forge FullStack Project - Local Setup and Run Guide

This README explains how to understand, build, run, debug, and test the Forge FullStack project locally.

The project contains a frontend Angular application, Java Spring Boot backend services, PostgreSQL, Kafka, Redis, Nginx gateway, and an external AI service integration used for JD generation.

---

## 1. Project Overview

The Forge FullStack project is a microservice-based application.

Main modules:

```text
Forge_FullStack 2/
├── Forge_backend/
│   ├── backend/
│   │   ├── job-posting-service/
│   │   └── user-auth-service/
│   └── nginx/
├── Forge_frontend/
│   └── apps/
│       └── demands/
├── docker-compose.yml
├── init-db.sql
└── .env
```

### Main Services

| Service             | Purpose                                       | Local Port                         |
| ------------------- | --------------------------------------------- | ---------------------------------- |
| Frontend            | Angular UI                                    | 4200                               |
| Nginx Gateway       | Routes frontend API calls to backend services | 8084 or configured gateway port    |
| User Auth Service   | Handles login/authentication/JWT              | 8085 mapped to internal 8081       |
| Job Posting Service | Handles demands, job postings, JD generation  | 8082                               |
| PostgreSQL          | Database                                      | 5432                               |
| Kafka               | Event streaming                               | 9092 / 29092 internally            |
| Kafka UI            | Kafka management UI                           | usually 8080 or configured port    |
| Redis               | Cache/session support                         | 6379                               |
| External AI Service | Used by JD generation                         | expected at 9999 or configured URL |

---

## 2. Prerequisites

Install the following before running the project:

### Required

```text
Docker Desktop
Docker Compose
Java 17
Maven
Node.js
npm
Angular CLI
Git
```

### Check versions

```bash
docker --version
docker-compose --version
java -version
mvn -version
node -v
npm -v
```

Java should be version 17.

---

## 3. Important Environment File

The root folder should contain a `.env` file.

Example:

```env
DB_USERNAME=""
DB_PASSWORD=""
JWT_SECRET=
AI_SERVICE_BASE_URL=http://host.docker.internal:9999
```

### Important note about AI_SERVICE_BASE_URL

If the AI service runs on your Mac machine at port `9999`, use:

```env
AI_SERVICE_BASE_URL=http://host.docker.internal:9999
```

Do **not** use this inside Docker:

```env
AI_SERVICE_BASE_URL=http://localhost:9999
```

Inside a Docker container, `localhost` means the container itself, not your Mac.

If Team 4 gives a remote AI service URL, use that instead:

```env
AI_SERVICE_BASE_URL=http://actual-team4-ai-host:actual-port
```

After changing `.env`, recreate the job-posting-service:

```bash
docker-compose up -d --force-recreate job-posting-service
```

Verify:

```bash
docker-compose exec job-posting-service printenv | grep AI_SERVICE_BASE_URL
```

Expected example:

```text
AI_SERVICE_BASE_URL=http://host.docker.internal:9999
```

---

## 4. Maven / JFrog Setup

The backend depends on a private Grid Dynamics Maven artifact:

```text
com.gridynamics.forge:forge-ai-guardrail:2.0.1
```

Because this dependency is hosted in JFrog, Maven needs credentials.

Check local Maven settings:

```bash
ls ~/.m2/settings.xml
grep -n "jfrog-artifactory" ~/.m2/settings.xml
```

Local Maven should build successfully:

```bash
cd "Forge_backend/backend/job-posting-service"
mvn -B clean package -DskipTests
```

If this succeeds locally but Docker build fails with `401 Unauthorized`, Docker does not have access to Maven credentials.

### Quick local Docker fix

From:

```bash
cd "Forge_backend/backend/job-posting-service"
```

Copy Maven settings:

```bash
cp ~/.m2/settings.xml ./settings.xml
```

Add `settings.xml` to `.gitignore`:

```bash
echo "settings.xml" >> .gitignore
```

Update the `Dockerfile` build stage like this:

```dockerfile
# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

RUN apt-get update -q && apt-get install -yq maven && \
    mvn -B -q clean package -DskipTests && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
```

Keep the runtime stage unchanged.

Important: never commit `settings.xml` because it contains credentials.

Check before committing:

```bash
git status --short
```

---

## 5. Build and Run Everything with Docker

From the project root:

```bash
cd "/Users/sanjakumar/Downloads/Forge_FullStack 2"
```

Build all services:

```bash
docker-compose build
```

Start all services:

```bash
docker-compose up -d
```

Or build and start together:

```bash
docker-compose up --build -d
```

Check running containers:

```bash
docker-compose ps
```

Expected important services:

```text
forge-frontend
forge-auth
forge-job-posting
forge-postgres
forge-kafka
forge-zookeeper
forge-redis
forge-nginx
```

---

## 6. Rebuild One Service

### Rebuild job-posting-service

Use this after changing backend Java code:

```bash
docker-compose build job-posting-service
docker-compose up -d --force-recreate job-posting-service
```

Full clean rebuild:

```bash
docker-compose build --no-cache job-posting-service
docker-compose up -d --force-recreate job-posting-service
```

### Rebuild frontend

```bash
docker-compose build frontend
docker-compose up -d --force-recreate frontend
```

Service names may vary slightly depending on `docker-compose.yml`.

---

## 7. Application URLs

### Frontend

Open:

```text
http://localhost:4200
```

This is the main UI.

### Auth Service Health

```bash
curl http://localhost:8085/actuator/health
```

Expected:

```json
{"status":"UP"}
```

Opening this in browser may show:

```json
{"error":"Unauthorized"}
```

That is normal if you hit a protected endpoint without login.

### Job Posting Service Health

```bash
curl http://localhost:8082/actuator/health
```

Expected:

```json
{"status":"UP"}
```

### Gateway Health

Depending on your gateway port:

```bash
curl http://localhost:8084/health
```

or:

```bash
curl http://localhost:8080/health
```

Expected:

```text
ok
```

---

## 8. Frontend API Flow

The Angular frontend usually calls APIs through the Nginx gateway.

Example:

```text
Frontend: http://localhost:4200
API Gateway: http://localhost:8084/api/...
```

For JD generation, browser Network tab may show:

```text
POST http://localhost:8084/api/job-postings/generate-jd
```

Nginx forwards `/api/` requests to:

```text
job-posting-service:8082
```

---

## 9. Backend API Flow for JD Generation

JD generation flow:

```text
Angular Frontend
    ↓
Nginx Gateway
    ↓
Job Posting Service
    ↓
External AI Service
```

The backend controller endpoint is:

```java
@PostMapping("/generate-jd")
public ResponseEntity<JdSectionsResponse> generateJd(@RequestBody GenerateJdRequest req)
```

The job-posting-service calls the external AI service using:

```java
String url = aiServiceBaseUrl + "/api/v1/ai/jd/generate";
return restTemplate.postForObject(url, request, JdGenerationResponse.class);
```

So the final AI URL becomes:

```text
${AI_SERVICE_BASE_URL}/api/v1/ai/jd/generate
```

Example:

```text
http://host.docker.internal:9999/api/v1/ai/jd/generate
```

---

## 10. JD Generation Troubleshooting

### Case 1: Browser shows mock/template JD

Check the browser Network tab.

Open Chrome DevTools:

```text
Right click page → Inspect → Network
```

Click:

```text
Generate JD with AI
```

Open the request:

```text
generate-jd
```

Check:

```text
Request URL
Status Code
Response
```

If the response itself contains template text, Angular is not creating the mock data. It is showing exactly what the backend returned.

### Case 2: AI service is not running

Test from Mac:

```bash
curl -i http://localhost:9999
```

Test from Docker container:

```bash
docker-compose exec job-posting-service sh -c "curl -i http://host.docker.internal:9999"
```

If you see:

```text
Connection refused
```

then no AI service is running on port `9999`.

### Case 3: Backend logs show connection refused

Check logs:

```bash
docker-compose logs job-posting-service --tail=150
```

If logs show:

```text
Caused by: java.net.ConnectException: Connection refused
```

then job-posting-service is correctly trying to call the AI service, but the AI service is not available.

Fix by starting the AI service or updating `.env`:

```env
AI_SERVICE_BASE_URL=http://actual-ai-service-host:actual-port
```

Then recreate:

```bash
docker-compose up -d --force-recreate job-posting-service
```

---

## 11. Manual API Tests

### Health checks

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
```

### JD generation direct test

This endpoint may require authentication.

Without auth, this may return:

```json
{"error":"Unauthorized"}
```

Request:

```bash
curl -i -X POST http://localhost:8082/job-postings/generate-jd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "roleTitle": "Java Developer",
    "department": "Engineering",
    "location": "Bangalore",
    "workMode": "Hybrid",
    "experienceYears": "3",
    "skillsRequired": ["Java", "Spring Boot", "PostgreSQL"],
    "seniorityLevel": "MID",
    "employmentType": "Full-time",
    "additionalContext": "Backend API development"
  }'
```

Through gateway:

```bash
curl -i -X POST http://localhost:8084/api/job-postings/generate-jd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "roleTitle": "Java Developer",
    "department": "Engineering",
    "location": "Bangalore",
    "workMode": "Hybrid",
    "experienceYears": "3",
    "skillsRequired": ["Java", "Spring Boot", "PostgreSQL"],
    "seniorityLevel": "MID",
    "employmentType": "Full-time",
    "additionalContext": "Backend API development"
  }'
```

---

## 12. Getting the Auth Token from Browser

If curl returns unauthorized, use the frontend login flow.

Then open Chrome DevTools:

```text
Inspect → Network
```

Click any authenticated API request.

Look for request header:

```text
Authorization: Bearer <token>
```

Copy the token and use it in curl:

```bash
-H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 13. Useful Docker Commands

### See running services

```bash
docker-compose ps
```

### See logs for all services

```bash
docker-compose logs --tail=100
```

### See logs for job service

```bash
docker-compose logs job-posting-service --tail=150
```

### Follow logs live

```bash
docker-compose logs -f job-posting-service
```

### Restart one service

```bash
docker-compose restart job-posting-service
```

### Recreate one service

```bash
docker-compose up -d --force-recreate job-posting-service
```

### Stop all services

```bash
docker-compose down
```

### Stop and remove volumes

Warning: this deletes database data.

```bash
docker-compose down -v
```

### Rebuild everything from scratch

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## 14. Database

PostgreSQL runs in Docker.

Typical environment:

```env
DB_USERNAME=forge
DB_PASSWORD=forge_secure_123
```

The job-posting-service connects internally to:

```text
jdbc:postgresql://postgres:5432/job_posting_db
```

Because the service runs inside Docker, it uses the Docker service name:

```text
postgres
```

not:

```text
localhost
```

### Connect to PostgreSQL container

```bash
docker-compose exec postgres psql -U forge
```

List databases:

```sql
\l
```

Connect to job posting DB:

```sql
\c job_posting_db
```

List tables:

```sql
\dt
```

---

## 15. Kafka

Kafka is used for service communication/events.

Internal Kafka bootstrap server:

```text
kafka:29092
```

Check Kafka logs:

```bash
docker-compose logs kafka --tail=100
```

Check job-posting-service Kafka logs:

```bash
docker-compose logs job-posting-service --tail=150
```

Kafka UI may be available depending on configured port.

Check:

```bash
docker-compose ps
```

Look for the exposed Kafka UI port.

---

## 16. Frontend Development Mode

If you want to run frontend locally outside Docker:

```bash
cd "Forge_frontend"
npm install
```

Then run the app, depending on package scripts:

```bash
npm start
```

or:

```bash
npx nx serve demands
```

or:

```bash
ng serve
```

Open:

```text
http://localhost:4200
```

Make sure the frontend environment points to the correct API base URL, usually the Nginx gateway:

```text
http://localhost:8084/api
```

---

## 17. Backend Local Development

Run job-posting-service locally:

```bash
cd "Forge_backend/backend/job-posting-service"
mvn spring-boot:run
```

If running locally outside Docker, database and Kafka settings must point to accessible services.

When backend runs locally, `localhost` refers to your Mac.

When backend runs inside Docker, `localhost` refers to the container.

This distinction is very important.

---

## 18. Common Problems and Fixes

### Problem: `401 Unauthorized` from backend

Reason:

```text
Request is missing JWT token.
```

Fix:

Login from frontend or include:

```text
Authorization: Bearer YOUR_TOKEN
```

### Problem: `Connection refused host.docker.internal:9999`

Reason:

```text
AI service is not running on Mac port 9999.
```

Fix:

Start AI service locally or update:

```env
AI_SERVICE_BASE_URL=http://actual-ai-host:actual-port
```

Then recreate job service:

```bash
docker-compose up -d --force-recreate job-posting-service
```

### Problem: Docker build fails with JFrog 401

Reason:

```text
Docker Maven build cannot access private JFrog dependency.
```

Fix:

Copy Maven settings into Docker build context for local use:

```bash
cd "Forge_backend/backend/job-posting-service"
cp ~/.m2/settings.xml ./settings.xml
echo "settings.xml" >> .gitignore
```

Update Dockerfile to copy settings into `/root/.m2/settings.xml`.

### Problem: Old mock/template JD still appears

Reason:

```text
Old Docker image may still be running.
```

Fix:

```bash
docker-compose build --no-cache job-posting-service
docker-compose up -d --force-recreate job-posting-service
```

Then check logs:

```bash
docker-compose logs job-posting-service --tail=150
```

### Problem: Browser directly opening backend URL shows unauthorized

This is normal for protected endpoints.

Example:

```text
http://localhost:8082/job-postings/generate-jd
```

The endpoint is a POST endpoint and requires authentication. Opening it in browser sends a GET request without auth.

Use frontend or curl with JWT token.

---

## 19. Recommended Startup Sequence

From root:

```bash
cd "/Users/sanjakumar/Downloads/Forge_FullStack 2"
```

Start all services:

```bash
docker-compose up -d
```

Check status:

```bash
docker-compose ps
```

Check health:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
```

Open frontend:

```text
http://localhost:4200
```

Login.

Use the application.

For JD generation, make sure AI service is available:

```bash
curl -i http://localhost:9999
```

or configure `.env` with the real AI service URL.

---

## 20. Cleanup Before Commit

Before committing code, check:

```bash
git status --short
```

Make sure these are not committed:

```text
settings.xml
.env with secrets
target/
node_modules/
```

Recommended `.gitignore` entries:

```gitignore
settings.xml
.env
target/
node_modules/
.DS_Store
```

---

## 21. Current Known JD Generation Requirement

JD generation requires an external AI service.

Current expected backend setting:

```env
AI_SERVICE_BASE_URL=http://host.docker.internal:9999
```

But this only works if the AI service is running on your Mac at port `9999`.

If no AI service is running, backend logs will show:

```text
java.net.ConnectException: Connection refused
```

In that case, the application is correctly wired, but the AI dependency is missing.

To fix, either:

1. Start the Team 4 AI service locally on port `9999`, or
2. Update `.env` with the real Team 4 AI service URL, then recreate job-posting-service.

---

## 22. Quick Command Summary

```bash
# Go to project root
cd "/Users/sanjakumar/Downloads/Forge_FullStack 2"

# Start all
docker-compose up -d

# Build all
docker-compose build

# Rebuild job service
docker-compose build --no-cache job-posting-service
docker-compose up -d --force-recreate job-posting-service

# Check services
docker-compose ps

# Check job service env
docker-compose exec job-posting-service printenv | grep AI_SERVICE_BASE_URL

# Check health
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health

# Check AI service from Mac
curl -i http://localhost:9999

# Check AI service from container
docker-compose exec job-posting-service sh -c "curl -i http://host.docker.internal:9999"

# Logs
docker-compose logs job-posting-service --tail=150

# Stop
docker-compose down
```

---

## 23. Final Notes

The frontend, gateway, and job-posting-service wiring are correct when:

```text
Frontend sends POST /api/job-postings/generate-jd
Nginx forwards /api/ to job-posting-service:8082
job-posting-service reads AI_SERVICE_BASE_URL
job-posting-service calls /api/v1/ai/jd/generate on the AI service
```

If JD generation fails with connection refused, the issue is not Angular and not Nginx. It means the external AI service is not reachable.

If JD generation returns template-like text, inspect the Network response first. The frontend displays whatever JSON the backend returns.
