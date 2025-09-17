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
- **Backend Lead + Scrum Master (You)**  
  - Owns Spring Boot APIs, MongoDB models, and LTI endpoints.  
  - Facilitates Scrum ceremonies, backlog, and sprint planning.  

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
- **Maven 3.8+**
- **Node.js 20+**
- **npm 10+**
- **Docker & Docker Compose**
- **Git**

### Quick Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/mjack6182/lab-signoff-app.git
   cd lab-signoff-app
   ```

2. **Run the setup script**
   ```bash
   chmod +x scripts/setup-dev.sh
   ./scripts/setup-dev.sh
   ```

3. **Start the services**
   
   **Terminal 1 - Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   
   **Terminal 2 - Frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

4. **Access the applications**
   - **Frontend**: http://localhost:3000
   - **Backend API**: http://localhost:8080
   - **MongoDB Admin**: http://localhost:8081

### Manual Setup

If you prefer to set up manually:

1. **Start Infrastructure**
   ```bash
   cd infra
   docker-compose up -d
   ```

2. **Setup Backend**
   ```bash
   cd backend
   mvn clean compile
   ```

3. **Setup Frontend**
   ```bash
   cd frontend
   npm install
   ```

## 🏗 Project Structure

```
lab-signoff-app/
├── backend/                 # Spring Boot application
│   ├── src/
│   │   ├── main/java/       # Java source code
│   │   ├── main/resources/  # Configuration files
│   │   └── test/java/       # Test files
│   └── pom.xml              # Maven configuration
├── frontend/                # React application
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── services/        # API service layer
│   │   └── types/           # TypeScript type definitions
│   ├── public/              # Static assets
│   └── package.json         # npm configuration
├── infra/                   # Infrastructure configuration
│   ├── docker-compose.yml   # Docker services
│   └── mongo-init.js        # Database initialization
├── scripts/                 # Utility scripts
│   ├── setup-dev.sh         # Development setup
│   └── build-prod.sh        # Production build
└── .github/
    └── workflows/           # CI/CD pipelines
        ├── backend.yml      # Backend workflow
        └── frontend.yml     # Frontend workflow
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
