# Service Setup & Run Guide

This document provides setup, installation, environment configuration, and execution instructions for each service in the **Plastic Waste Reporting and Volunteer Tracking System**.

---

## Prerequisites Summary

Ensure the following tools are installed on your machine:
- **Node.js**: v18+ (Verified: v24.14.0) & **npm** (Verified: 11.10.1)
- **Java**: JDK 21+ or JDK 24 (Verified: Oracle JDK 24.0.1)
- **Maven**: 3.9+ (Verified: 3.9.16)
- **Python**: 3.10+ (Verified: 3.14.6)
- **PostgreSQL**: 15+ (Verified: 18 running on port 5432)

---

## 1. Backend Service (`backend/`)

The backend is built with **Java + Spring Boot 3.4.3 + Spring Security 6.4 + Flyway + PostgreSQL**.

### Environment Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Set environment variables (or copy template):
   ```powershell
   $env:DB_URL="jdbc:postgresql://localhost:5432/plastic_waste_db"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="your_postgres_password"
   ```
3. Run automated tests (runs H2 in-memory test suite):
   ```bash
   mvn test
   ```

### Running the Application & Flyway Migrations
```bash
mvn spring-boot:run
```
- **Service URL**: `http://localhost:8080`
- **Health Check URL**: `http://localhost:8080/api/v1/health`
- **Spring Actuator URL**: `http://localhost:8080/actuator/health`

### Initial Admin Account Creation
To seed the initial administrator without hardcoding credentials:
```powershell
$env:ADMIN_INITIAL_EMAIL="admin@plasticwaste.org"
$env:ADMIN_INITIAL_PASSWORD="YourSecureAdminPassword2026!"
mvn spring-boot:run
```

---

## 2. Frontend Service (`frontend/`)

The frontend is a single-page web app built with **React 19 + Vite + Vanilla CSS + Leaflet**.

### Running Development Server
```bash
cd frontend
npm run dev
```
- **Application URL**: `http://localhost:5173`

---

## 3. AI Verification Microservice (`ai-service/`)

FastAPI microservice for plastic waste image verification.

### Running Service
```bash
cd ai-service
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```
- **Base URL**: `http://localhost:8001`
- **Health Endpoint**: `http://localhost:8001/health`
- **Swagger Docs**: `http://localhost:8001/docs`

---

## 4. Postman API Testing

An exportable Postman collection is available at:
`docs/postman/Plastic_Waste_API_Collection.json`

### Key Endpoints:
- `POST /api/v1/auth/register` — Citizen & Volunteer Registration (Public)
- `POST /api/v1/auth/login` — Authentication (Returns Bearer Token & HttpOnly Cookie)
- `POST /api/v1/complaints` — Submit Plastic Waste Complaint with Photos & Coordinates (`ROLE_CITIZEN`)
- `GET /api/v1/complaints/my` — View Citizen's Filed Complaints (`ROLE_CITIZEN`)
- `PATCH /api/v1/complaints/{id}/review` — Admin Triage (`ROLE_ADMIN`)
- `PATCH /api/v1/complaints/{id}/verify` — Admin Verification (`ROLE_ADMIN`)
- `PATCH /api/v1/complaints/{id}/reject` — Admin Rejection (`ROLE_ADMIN`)
- `PATCH /api/v1/complaints/{id}/cancel` — Citizen Cancellation (`ROLE_CITIZEN`)
- `GET /api/v1/notifications` — View Notifications (Authenticated)
