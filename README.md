# 📘 Lab Sign-Off App

## Overview  
The Lab Sign-Off App is a **teacher-facing Canvas LTI 1.3 tool** for managing lab checkpoints.  
Instructors and TAs can **Pass/Return student group checkpoints in real time**, with progress automatically synced to **Canvas Gradebook**.  
The app is installable as a **PWA** and supports **multi-staff realtime collaboration** via WebSockets.  

---

## 🎯 Features  
- Launch app directly from Canvas using **LTI 1.3**.  
- Import course roster via **NRPS** and auto-create groups.  
- Teacher/TA **Pass or Return checkpoints** with instant updates.  
- **Automatic grade sync** with Canvas using AGS.  
- **Realtime updates** across teacher + TA devices.  
- **PWA support** (installable on phone/tablet).  
- MongoDB persistence + audit log export.  

---

## 🛠 Tech Stack  
- **Frontend:** React + Vite + TypeScript, React Query, STOMP WebSockets, vite-plugin-pwa  
- **Backend:** Spring Boot (Java 21), MongoDB Atlas, WebSockets (STOMP), Spring Security + LTI 1.3/OIDC  
- **Database:** MongoDB (Docker locally, Atlas in production)  
- **Infra:** Docker Compose, Railway/Render for deployment  
- **CI/CD:** GitHub Actions (frontend & backend pipelines)  

---

## 👥 Team Roles  
- **Backend Lead**  
  - Owns Spring Boot APIs, MongoDB models, and LTI endpoints.  

- **LTI/Canvas Integration Lead**  
  - Handles LTI launch flow, JWT validation, NRPS roster import, AGS grade sync.  

- **Realtime/Infra Lead**  
  - Builds WebSocket infrastructure, Docker Compose, and deployment pipelines.  

- **Frontend UX/PWA Lead**  
  - Designs React + Vite dashboard UI, mobile-first layouts, and PWA install.  

- **Frontend Features/Polish Lead**  
  - Adds filters, group editing, audit logs, and ensures responsive UI polish.  

*(Each role is responsible for testing & documenting their own work.)*  

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

- **Java 21+** (OpenJDK recommended)
- **Gradle - Groovy**
- **Node.js 20+**
- **npm 10+**
- **Docker & Docker Compose**
- **Git**



## 📅 Sprint Timeline (2-Week Sprints)

- **Sprint 1:** Repo setup, Mongo + API scaffold, React+Vite dashboard with mock groups.  
- **Sprint 2:** Canvas LTI launch (JWT validation), groups from DB, role-based access.  
- **Sprint 3:** Pass/Return checkpoints → DB + WebSocket broadcast.  
- **Sprint 4:** Roster import via NRPS, auto group creation, group editing UI.  
- **Sprint 5:** Canvas AGS grade sync, multi-TA realtime support, filters.  
- **Sprint 6:** PWA polish (installable/offline), audit logs, deployment & final demo.  

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
