# Lab Signoff App - Deployment Guide

This guide covers the deployment process for the Lab Signoff App, including both frontend (Firebase Hosting) and backend deployments.

## Prerequisites

- Node.js (v18 or higher)
- Firebase CLI installed globally: `npm install -g firebase-tools`
- Access to the Firebase project: `lab-signoff-app`
- Java 21 for backend deployment

## Frontend Deployment (Firebase Hosting)

### 1. Build the Frontend Application

Navigate to the frontend directory and build the production bundle:

```bash
cd frontend
npm run build
```

**Expected Output:**
```
> lab_signoff_app@0.0.0 build
> vite build

vite v7.1.7 building for production...
✓ 150 modules transformed.
dist/index.html                   0.38 kB │ gzip:   0.27 kB
dist/assets/index-BZZoIrRg.css   41.41 kB │ gzip:   7.57 kB
dist/assets/index-qf_dMmD4.js   319.05 kB │ gzip: 100.36 kB
✓ built in 754ms
```

### 2. Initialize Firebase Hosting (First Time Only)

If not already configured, initialize Firebase hosting:

```bash
firebase init hosting
```

**Configuration Settings:**
- Project: `lab-signoff-app (Lab-signoff-app)`
- Public directory: `dist`
- Single-page app: `Yes`
- GitHub auto-deploy: `No` (manual deployment)
- Overwrite existing index.html: `No`

**Expected Output:**
```
✔ What do you want to use as your public directory? dist
✔ Configure as a single-page app (rewrite all urls to /index.html)? Yes
✔ Set up automatic builds and deploys with GitHub? No
✔ File dist/index.html already exists. Overwrite? No
i Skipping write of dist/index.html

✔ Wrote configuration info to firebase.json
✔ Wrote project information to .firebaserc

✔ Firebase initialization complete!
```

### 3. Deploy to Firebase Hosting

Deploy the built application:

```bash
firebase deploy --only hosting
```

**Expected Output:**
```
=== Deploying to 'lab-signoff-app'...

i  deploying hosting
i  hosting[lab-signoff-app]: beginning deploy...
i  hosting[lab-signoff-app]: found 4 files in dist
✔  hosting[lab-signoff-app]: file upload complete
i  hosting[lab-signoff-app]: finalizing version...
✔  hosting[lab-signoff-app]: version finalized
i  hosting[lab-signoff-app]: releasing new version...
✔  hosting[lab-signoff-app]: release complete

✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/lab-signoff-app/overview
Hosting URL: https://lab-signoff-app.web.app
```

### 4. Access the Deployed Application

- **Live URL:** https://lab-signoff-app.web.app
- **Firebase Console:** https://console.firebase.google.com/project/lab-signoff-app/overview

## Backend Deployment

### Local Development

1. **Start the Backend Server:**
   ```bash
   cd backend
   ./gradlew bootRun
   ```

2. **Check Port Availability:**
   If port 8080 is in use, find and kill the process:
   ```bash
   # Find process using port 8080
   lsof -i :8080
   
   # Kill the process (replace PID with actual process ID)
   kill -9 <PID>
   ```

3. **Alternative Port Configuration:**
   Add to `backend/src/main/resources/application.yml`:
   ```yaml
   server:
     port: 8081
   ```

### Production Deployment Options

#### Option 1: Heroku Deployment
```bash
# Install Heroku CLI
# Login to Heroku
heroku login

# Create Heroku app
heroku create lab-signoff-backend

# Deploy
git subtree push --prefix backend heroku main
```

#### Option 2: Docker Deployment
```bash
# Build Docker image
cd backend
docker build -t lab-signoff-backend .

# Run container
docker run -p 8080:8080 lab-signoff-backend
```

## Environment Configuration

### Frontend Environment Variables

Create `.env.production` in the frontend directory:
```env
VITE_API_BASE_URL=https://your-backend-url.com
VITE_FIREBASE_API_KEY=your-firebase-api-key
```

### Backend Environment Variables

Configure in `application-prod.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
server:
  port: ${PORT:8080}
cors:
  allowed-origins: 
    - https://lab-signoff-app.web.app
```

## Complete Deployment Workflow

### Quick Deployment Script

Create a deployment script `deploy.sh`:
```bash
#!/bin/bash
echo "Building frontend..."
cd frontend
npm run build

echo "Deploying to Firebase..."
firebase deploy --only hosting

echo "Frontend deployed successfully!"
echo "URL: https://lab-signoff-app.web.app"
```

Make it executable:
```bash
chmod +x deploy.sh
./deploy.sh
```

### Full Production Deployment Checklist

- [ ] Run frontend tests: `npm test`
- [ ] Run backend tests: `./gradlew test`
- [ ] Build frontend: `npm run build`
- [ ] Verify build output in `dist/` directory
- [ ] Deploy to Firebase: `firebase deploy --only hosting`
- [ ] Deploy backend to production server
- [ ] Update environment variables
- [ ] Test production endpoints
- [ ] Verify CORS configuration
- [ ] Monitor deployment logs

## Rollback Procedures

### Frontend Rollback
```bash
# View deployment history
firebase hosting:channel:list

# Rollback to previous version
firebase hosting:channel:rollback <channel-id>
```

### Backend Rollback
```bash
# Using Heroku
heroku rollback

# Using Docker
docker run -p 8080:8080 lab-signoff-backend:previous-tag
```

## Monitoring and Logs

### Frontend Monitoring
- Firebase Console: https://console.firebase.google.com/project/lab-signoff-app/hosting
- Analytics and performance metrics available in Firebase

### Backend Monitoring
```bash
# Local logs
./gradlew bootRun --info

# Production logs (Heroku example)
heroku logs --tail -a lab-signoff-backend
```

## Troubleshooting

### Common Issues

1. **Port 8080 already in use:**
   ```bash
   lsof -i :8080
   kill -9 <PID>
   ```

2. **Firebase authentication errors:**
   ```bash
   firebase login
   firebase use lab-signoff-app
   ```

3. **Build failures:**
   ```bash
   # Clear npm cache
   npm cache clean --force
   
   # Reinstall dependencies
   rm -rf node_modules package-lock.json
   npm install
   ```

4. **CORS errors in production:**
   - Verify backend CORS configuration includes frontend URL
   - Check environment variables are set correctly

## Support

For deployment issues:
1. Check Firebase Console for hosting errors
2. Review backend application logs
3. Verify environment variable configuration
4. Test API endpoints independently

---

**Last Updated:** October 23, 2025  
**Deployment URL:** https://lab-signoff-app.web.app