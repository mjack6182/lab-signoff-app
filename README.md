# Lab Signoff Application

A comprehensive laboratory signoff system built as a modern web application with Spring Boot backend and React frontend, designed to streamline the process of student lab work validation and instructor signoffs in educational environments.

## 🎯 Overview

The Lab Signoff Application provides a digital solution for managing laboratory exercises, student submissions, and instructor evaluations. It supports LTI 1.3 integration for seamless LMS connectivity and offers real-time collaboration features through WebSockets.

### Key Features

- **Student Portal**: Submit lab work, track progress, and receive feedback
- **Instructor Dashboard**: Review submissions, provide signoffs, and manage labs
- **Real-time Notifications**: WebSocket-powered updates for instant communication
- **LTI 1.3 Integration**: Seamless integration with Learning Management Systems
- **Progressive Web App**: Works offline and provides native app-like experience
- **Responsive Design**: Optimized for desktop, tablet, and mobile devices

## 🛠 Technology Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data MongoDB** - Database integration
- **Spring WebSocket** - Real-time communication
- **Maven** - Dependency management and build tool
- **MongoDB** - NoSQL database

### Frontend
- **React 18** - UI library
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool and dev server
- **React Query** - Server state management
- **React Router** - Client-side routing
- **Vite PWA Plugin** - Progressive Web App capabilities

### Infrastructure
- **Docker & Docker Compose** - Containerization and orchestration
- **MongoDB 7.0** - Database server
- **Mongo Express** - Database administration interface

### DevOps
- **GitHub Actions** - CI/CD pipelines
- **Maven** - Java build automation
- **npm** - Node.js package management

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

## 👥 Team Roles & Responsibilities

### 🎯 Product Owner
- Define requirements and user stories
- Prioritize features and backlog
- Accept/reject completed work
- Liaise with stakeholders

### 🛠 Tech Lead / Senior Developer
- Architecture decisions and technical direction
- Code review and quality standards
- Mentoring junior developers
- DevOps and deployment strategies

### 💻 Backend Developer
- Spring Boot application development
- Database design and optimization
- API design and implementation
- Security and authentication

### 🎨 Frontend Developer
- React application development
- UI/UX implementation
- PWA features and optimization
- Client-side state management

### ⚙️ DevOps Engineer
- CI/CD pipeline management
- Infrastructure setup and monitoring
- Docker containerization
- Deployment automation

### 🧪 QA Engineer
- Test strategy and planning
- Automated test implementation
- Manual testing and bug reporting
- Performance testing

## 🔧 Development Workflow

### Running Tests

**Backend Tests:**
```bash
cd backend
mvn test
```

**Frontend Tests:**
```bash
cd frontend
npm run test
```

### Code Quality

**Frontend Linting:**
```bash
cd frontend
npm run lint
```

**TypeScript Check:**
```bash
cd frontend
npx tsc --noEmit
```

### Building for Production

```bash
# Build everything
./scripts/build-prod.sh

# Or build individually
cd backend && mvn clean package
cd frontend && npm run build
```

## 🔐 Environment Configuration

### Backend Configuration

Create `backend/src/main/resources/application-local.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/labsignoff
  security:
    oauth2:
      client:
        registration:
          lti:
            client-id: your-lti-client-id
            client-secret: your-lti-client-secret
```

### Frontend Configuration

Create `frontend/.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

## 📊 Monitoring & Observability

- **Application Logs**: Check console output for both frontend and backend
- **Database Monitoring**: Use Mongo Express at http://localhost:8081
- **Health Checks**: Backend health endpoint at `/actuator/health`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue on GitHub
- Check the documentation in the `/docs` folder
- Contact the development team

---

**Happy Coding! 🚀**