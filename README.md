# Plastic Waste Reporting and Volunteer Tracking System

A full-stack, distributed platform empowering citizens to report plastic waste hotspots, enabling local administrators to triage and dispatch cleanup tasks, and tracking volunteer resolution workflows with before-and-after proof of work.

---

## Architecture Overview

The system is organized as a modular monorepo consisting of three independently runnable services and centralized documentation:

```
plastic-waste-management/
├── frontend/        # React 19 + Vite + Vanilla CSS SPA with Leaflet Maps
├── backend/         # Java + Spring Boot 3.4.x + Maven REST API & Flyway Migrations
├── ai-service/      # Python + FastAPI microservice for image verification (Scaffolded)
└── docs/            # System Architecture, Database Schemas & Run guides
```

### Architectural Highlights
- **Decoupled Architecture**: Frontend, Backend, and AI Service can be developed, tested, and deployed independently.
- **Strict Role-Based Access Control**:
  - `CITIZEN`: Submit reports with geolocation, photos, and track resolution.
  - `ADMIN`: Triage reports, view interactive waste map, search nearby volunteers, and dispatch tasks.
  - `VOLUNTEER`: Toggle availability, share GPS coordinates, accept/decline tasks, and submit cleanup proof.
- **Audit-Logged Lifecycle**:
  - `SUBMITTED` ➔ `UNDER_REVIEW` ➔ `VERIFIED` ➔ `ASSIGNED` ➔ `ACCEPTED` ➔ `IN_PROGRESS` ➔ `RESOLVED`.
  - Robust handling of task declination, citizen cancellation, and admin reassignment safeguards.
- **Cloud-Ready Media & Spatial Queries**: Geocoded map visualization via Leaflet + OpenStreetMap and media storage via Cloudinary.

---

## Quickstart & Service Run Guide

Each service runs on its own dedicated port:

| Service | Directory | Tech Stack | Port | Command to Run | Health Check URL |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Backend** | `backend/` | Spring Boot 3.4 | `8080` | `mvn spring-boot:run` | `http://localhost:8080/api/v1/health` |
| **Frontend** | `frontend/` | React + Vite | `5173` | `npm run dev` | `http://localhost:5173` |
| **AI Service** | `ai-service/` | FastAPI | `8001` | `uvicorn app.main:app --port 8001` | `http://localhost:8001/health` |

For detailed step-by-step setup guides, refer to [docs/README.md](docs/README.md).  
For the full architectural specification and ER diagrams, refer to [docs/architecture.md](docs/architecture.md).

---

## Phased Project Milestones

- [x] **Milestone 1**: Scaffolding, verified tool compatibility, independent service health endpoints & documentation.
- [ ] **Milestone 2**: PostgreSQL schema, Flyway migrations, and Spring Security JWT authentication.
- [ ] **Milestone 3**: Complaint submission, Cloudinary image upload, and state machine transitions.
- [ ] **Milestone 4**: Volunteer GPS tracking, Haversine nearby dispatcher, and assignment workflow.
- [ ] **Milestone 5**: Interactive React frontend with Leaflet waste map and role-specific dashboards.
- [ ] **Milestone 6**: AI image classification microservice and end-to-end Postman test suites.
