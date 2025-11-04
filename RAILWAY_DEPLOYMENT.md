# Railway Deployment Guide - Lab Signoff App

This guide will walk you through deploying the Lab Signoff App to Railway.app with separate services for frontend, backend, and Redis.

## Prerequisites

- A Railway.app account (sign up at https://railway.app)
- Railway CLI installed (optional): `npm i -g @railway/cli`
- Your GitHub repository pushed and ready

## Architecture Overview

Your Railway project will have **3 services**:
1. **Backend** - Spring Boot (Java 21 + Gradle)
2. **Frontend** - React/Vite (served via Nginx)
3. **Redis** - Managed Redis database

Plus your existing **MongoDB Atlas** instance.

---

## Quick Deployment Steps

### Step 1: Create Railway Project

1. Go to https://railway.app/dashboard
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Connect and select your repository
5. Railway creates an initial service (we'll use this for backend)

### Step 2: Add Redis Database

1. In your Railway project, click **"New"** → **"Database"** → **"Add Redis"**
2. Railway auto-provisions Redis with these environment variables:
   - `REDIS_URL`
   - `REDIS_PRIVATE_URL` (use this for internal connections)
   - `REDIS_HOST`
   - `REDIS_PORT`

### Step 3: Configure Backend Service

#### A. Set Root Directory
1. Select the backend service
2. Go to **Settings** → **General** → **Root Directory**
3. Set to: `backend`

#### B. Add Environment Variables
Go to **Variables** tab and add:

```bash
# Spring Configuration
SPRING_APPLICATION_NAME=lab-signoff
SERVER_PORT=8080

# MongoDB Atlas (replace with your actual connection string)
SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/lab-signoff?retryWrites=true&w=majority

# Redis (use Railway's internal private URL)
REDIS_HOST=${{Redis.REDIS_PRIVATE_URL}}
REDIS_PORT=6379

# LTI Configuration (from Canvas Developer Key)
LTI_CLIENT_ID=your_canvas_lti_client_id_here

# CORS - will update this after frontend is deployed
CORS_ALLOWED_ORIGINS=https://your-frontend-url.railway.app
```

#### C. Enable Public Networking
1. Go to **Settings** → **Networking**
2. Enable **"Public Networking"**
3. **Copy the public URL** (e.g., `https://backend-production-abcd1234.up.railway.app`)

The Dockerfile at `backend/Dockerfile` will be automatically detected.

### Step 4: Add Frontend Service

#### A. Create Service from Same Repo
1. Click **"New"** → **"GitHub Repo"**
2. Select the **same repository**
3. Go to **Settings** → **General** → **Root Directory**
4. Set to: `frontend`

#### B. Add Environment Variables
Go to **Variables** tab and add:

```bash
# Backend URL (use the backend's Railway URL from Step 3)
VITE_API_URL=https://backend-production-abcd1234.up.railway.app
BACKEND_URL=https://backend-production-abcd1234.up.railway.app
```

#### C. Enable Public Networking
1. Go to **Settings** → **Networking**
2. Enable **"Public Networking"**
3. **Copy the public URL** - this is your main app URL

The Dockerfile at `frontend/Dockerfile` will be automatically detected.

### Step 5: Update Backend CORS

Now that you have the frontend URL:

1. Go back to **Backend service** → **Variables**
2. Update `CORS_ALLOWED_ORIGINS`:
   ```bash
   CORS_ALLOWED_ORIGINS=https://frontend-production-xyz789.up.railway.app
   ```
3. The backend will automatically redeploy

---

## Environment Variables Reference

### Backend Service
| Variable | Example | Required |
|----------|---------|----------|
| `SPRING_APPLICATION_NAME` | `lab-signoff` | Yes |
| `SERVER_PORT` | `8080` | Yes |
| `SPRING_DATA_MONGODB_URI` | `mongodb+srv://...` | Yes |
| `REDIS_HOST` | `${{Redis.REDIS_PRIVATE_URL}}` | Yes |
| `REDIS_PORT` | `6379` | Yes |
| `LTI_CLIENT_ID` | `your_client_id` | Yes |
| `CORS_ALLOWED_ORIGINS` | `https://frontend.railway.app` | Yes |

### Frontend Service
| Variable | Example | Required |
|----------|---------|----------|
| `VITE_API_URL` | `https://backend.railway.app` | Yes |
| `BACKEND_URL` | `https://backend.railway.app` | Yes |

### Redis Service
No configuration needed - fully managed by Railway.

---

## Automatic Deployments

Railway automatically deploys when you push to your connected branch:

```bash
git add .
git commit -m "Your changes"
git push origin main
```

Railway will:
- Detect changes
- Build using Dockerfiles
- Deploy new versions
- Show build logs in real-time

---

## Monitoring & Debugging

### View Logs
1. Click on any service
2. Go to **"Deployments"** tab
3. Select the deployment
4. Click **"View Logs"**

### Check Metrics
- Each service has a **"Metrics"** tab
- Shows CPU, memory, network usage
- Real-time and historical data

### Common Issues

#### ❌ Frontend Build Fails: `npm ci` error
**Error**: `npm ci can only install packages when your package.json and package-lock.json are in sync`

**Solution**:
- The Dockerfile now uses `npm install --frozen-lockfile` instead
- Ensure `package-lock.json` is committed to git
- If needed, run `npm install` locally and commit the lock file

#### ❌ Backend Won't Start
**Check**:
- Environment variables are set correctly
- MongoDB connection string is valid (test it locally)
- Java version is 21 (Dockerfile specifies this)
- Check logs for Spring Boot errors

#### ❌ Frontend Can't Connect to Backend
**Check**:
- `VITE_API_URL` points to correct backend URL
- Backend's `CORS_ALLOWED_ORIGINS` includes frontend URL
- Both services are running (green status)
- Check browser console for CORS errors

#### ❌ Redis Connection Fails
**Solution**:
- Use `${{Redis.REDIS_PRIVATE_URL}}` for internal connections
- Verify Redis service is running
- Check network connectivity in logs

#### ❌ WebSocket Connections Fail
**Check**:
- Nginx properly proxies WebSocket upgrades (configured in frontend Dockerfile)
- Backend WebSocket endpoint is accessible
- Check `/ws/` path in logs

---

## Build Configuration Summary

### Backend (`backend/Dockerfile`)
- **Base Image**: `eclipse-temurin:21-jdk-alpine`
- **Build Tool**: Gradle
- **Port**: 8080
- **Multi-stage**: Yes (build + runtime)

### Frontend (`frontend/Dockerfile`)
- **Build Image**: `node:20-alpine`
- **Serve Image**: `nginx:alpine`
- **Port**: 80
- **Features**: SPA routing, API proxying, WebSocket support

---

## Advanced Configuration

### Custom Domains
1. Select frontend service
2. **Settings** → **Domains**
3. Click **"Add Domain"**
4. Follow DNS configuration instructions
5. Update backend's `CORS_ALLOWED_ORIGINS`

### Scaling
1. **Settings** → **Deploy**
2. Adjust **"Replicas"** count
3. Note: May require session management updates

### Health Checks
Railway automatically monitors:
- HTTP endpoint availability
- Container health
- Restart on failure (max 10 retries)

### Cost Management
- Monitor usage in billing dashboard
- Use sleep mode for dev/staging environments
- Check resource usage in metrics

---

## Testing Your Deployment

### 1. Check Backend Health
```bash
curl https://your-backend-url.railway.app/actuator/health
```

### 2. Check Frontend
Visit: `https://your-frontend-url.railway.app`

### 3. Check Redis Connection
Look in backend logs for Redis connection success messages

### 4. Test API Calls
Open browser console and check Network tab for API calls to backend

---

## Railway CLI (Optional)

Install and use Railway CLI for advanced workflows:

```bash
# Install
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# View logs
railway logs

# Set variables
railway variables set KEY=value

# Deploy
railway up
```

---

## Troubleshooting Checklist

- [ ] All environment variables are set correctly
- [ ] Backend and frontend services are both running (green)
- [ ] Redis service is running
- [ ] MongoDB Atlas connection string is correct
- [ ] CORS is configured with correct frontend URL
- [ ] Public networking is enabled on backend and frontend
- [ ] Check logs for specific error messages
- [ ] Verify Dockerfiles are in correct locations

---

## Support Resources

- **Railway Docs**: https://docs.railway.app
- **Railway Discord**: https://discord.gg/railway
- **Railway Status**: https://status.railway.app

---

## Next Steps After Deployment

1. ✅ Test all features thoroughly
2. ✅ Set up monitoring and alerts
3. ✅ Configure custom domain (optional)
4. ✅ Set up staging environment
5. ✅ Configure CI/CD pipelines
6. ✅ Review security settings
7. ✅ Set up backup strategy for data

---

## Quick Reference: Service URLs

After deployment, note these URLs:

- **Frontend**: `https://frontend-production-[id].up.railway.app`
- **Backend**: `https://backend-production-[id].up.railway.app`
- **Redis**: Internal only (via private URL)
- **MongoDB**: External (MongoDB Atlas)

Keep these handy for configuration and testing!
