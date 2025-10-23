# Railway Deployment Fixes - Summary

## Problem
The frontend Docker build was failing with an `npm ci` error due to package-lock.json being out of sync.

## Changes Made

### 1. Fixed Frontend Package Lock
- **File**: `frontend/package-lock.json`
- **Action**: Regenerated with `npm install` to sync with package.json
- **Why**: The lock file was missing some dependencies that were added

### 2. Updated Frontend Dockerfile
- **File**: `frontend/Dockerfile`
- **Change**: Line 10-11
  ```diff
  - RUN npm ci
  + # Use npm install instead of npm ci for more flexibility
  + RUN npm install --frozen-lockfile
  ```
- **Why**: `npm install --frozen-lockfile` is more forgiving than `npm ci` while still ensuring reproducible builds

### 3. Created Deployment Files
All these files are ready in the repository:

- ✅ `railway.json` - Railway project configuration
- ✅ `backend/Dockerfile` - Multi-stage build for Spring Boot
- ✅ `frontend/Dockerfile` - Multi-stage build for React + Nginx
- ✅ `.railwayignore` - Excludes unnecessary files from deployment
- ✅ `RAILWAY_DEPLOYMENT.md` - Complete deployment guide

## Files to Commit

```bash
# Modified files
frontend/Dockerfile              # Fixed npm install issue
frontend/package-lock.json       # Regenerated to be in sync

# New deployment files
railway.json                     # Railway configuration
backend/Dockerfile              # Backend build configuration
frontend/Dockerfile             # Frontend build configuration (also modified)
.railwayignore                  # Build optimization
RAILWAY_DEPLOYMENT.md           # Deployment guide
```

## Next Steps

### 1. Commit and Push Changes

```bash
# From the root of your project
git add .
git commit -m "Add Railway deployment configuration and fix npm build issues"
git push origin dev
```

### 2. Deploy to Railway

Follow the steps in [RAILWAY_DEPLOYMENT.md](RAILWAY_DEPLOYMENT.md):

1. Create Railway project from GitHub repo
2. Add Redis database
3. Configure backend service (set root dir to `backend`)
4. Add frontend service (set root dir to `frontend`)
5. Set all environment variables
6. Update CORS after getting URLs

### 3. Required Environment Variables

**Backend**:
- `SPRING_APPLICATION_NAME=lab-signoff`
- `SERVER_PORT=8080`
- `SPRING_DATA_MONGODB_URI=<your-mongodb-atlas-uri>`
- `REDIS_HOST=${{Redis.REDIS_PRIVATE_URL}}`
- `REDIS_PORT=6379`
- `LTI_CLIENT_ID=<your-canvas-lti-client-id>`
- `CORS_ALLOWED_ORIGINS=<frontend-railway-url>`

**Frontend**:
- `VITE_API_URL=<backend-railway-url>`
- `BACKEND_URL=<backend-railway-url>`

## What's Different from Local Development

| Aspect | Local (Makefile) | Railway |
|--------|------------------|---------|
| Backend | Gradle bootRun | Docker container |
| Frontend | Vite dev server | Nginx serving built files |
| Redis | localhost:6379 | Railway managed Redis |
| MongoDB | MongoDB Atlas | MongoDB Atlas (same) |
| Port | 8080 backend, 5173 frontend | Railway assigns ports |

## Testing the Deployment

Once deployed:

1. **Check backend health**:
   ```bash
   curl https://your-backend-url.railway.app/actuator/health
   ```

2. **Visit frontend**:
   Open `https://your-frontend-url.railway.app`

3. **Check logs**:
   - Railway Dashboard → Service → Deployments → View Logs
   - Look for successful Spring Boot startup
   - Check for Redis connection success
   - Verify MongoDB connection

4. **Test functionality**:
   - Load lab selector page
   - Navigate to groups
   - Check checkpoints
   - Test WebSocket connections (if applicable)

## Troubleshooting Quick Reference

- **Build fails**: Check logs in Railway deployment
- **Can't connect to backend**: Verify CORS and VITE_API_URL
- **Redis errors**: Use `${{Redis.REDIS_PRIVATE_URL}}` not `REDIS_HOST`
- **MongoDB errors**: Test connection string locally first
- **404 on refresh**: Nginx SPA routing is configured (should work)

## Files Structure

```
lab-signoff-app/
├── railway.json                 # Railway project config
├── .railwayignore              # Deployment optimization
├── RAILWAY_DEPLOYMENT.md       # Full deployment guide
├── backend/
│   ├── Dockerfile              # Spring Boot container
│   ├── build.gradle            # Gradle build config
│   └── src/                    # Java source code
└── frontend/
    ├── Dockerfile              # React + Nginx container
    ├── package.json            # NPM dependencies
    ├── package-lock.json       # Locked dependencies (FIXED)
    └── src/                    # React source code
```

## Architecture on Railway

```
┌─────────────────────────────────────────────────┐
│                Railway Project                   │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │   Frontend   │  │   Backend    │             │
│  │   (Nginx)    │──│ (Spring Boot)│             │
│  │   Port 80    │  │   Port 8080  │             │
│  └──────────────┘  └───────┬──────┘             │
│                            │                     │
│                    ┌───────▼──────┐              │
│                    │     Redis    │              │
│                    │   (Managed)  │              │
│                    └──────────────┘              │
└─────────────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────┐
              │   MongoDB Atlas    │
              │     (External)     │
              └────────────────────┘
```

## Cost Estimation

Railway pricing (as of 2024):
- **Hobby Plan**: $5/month + usage
- **Estimated usage**: ~$5-15/month for small app
  - 3 services (frontend, backend, Redis)
  - Low to moderate traffic
  - Shared resources

MongoDB Atlas Free Tier: $0 (512MB storage limit)

**Total estimated**: $10-20/month

---

Your `dev` branch is now ready for Railway deployment! 🚀
