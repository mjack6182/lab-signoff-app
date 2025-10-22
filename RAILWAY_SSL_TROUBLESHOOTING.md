# Railway SSL Certificate Error - Troubleshooting Guide

## Error Description
`ERR_CERT_AUTHORITY_INVALID` when accessing `lab-signoff-app-development.up.railway.app`

This error with Railway's `*.up.railway.app` domains is unusual because Railway automatically provides valid SSL certificates.

## Root Causes (Most Likely to Least)

### 1. Service Not Running (Most Common)
**Symptoms**: SSL error, can't access site
**Cause**: The application crashed or failed to start, Railway shows error page with invalid cert
**How to Check**:
- Railway Dashboard → Your Service → Check status indicator (should be green)
- Deployments tab → Latest deployment should show "Success" and "Active"
- Logs should show application running

**Fix**:
- Check application logs for errors
- Verify all environment variables are set
- Ensure application starts successfully

### 2. Certificate Provisioning in Progress
**Symptoms**: SSL error immediately after first deployment
**Cause**: Railway is still provisioning SSL certificate (takes 2-5 minutes)
**How to Check**:
- Check if deployment just happened
- Wait 5 minutes and try again

**Fix**: Wait for certificate provisioning to complete

### 3. Wrong Domain/URL
**Symptoms**: SSL error on specific URL
**Cause**: Using an old or incorrect Railway URL
**How to Check**:
- Railway Dashboard → Service → Settings → Domains
- Compare URL in browser with URL shown in Railway

**Fix**: Use the exact URL shown in Railway dashboard

### 4. Railway Platform Issue
**Symptoms**: SSL error across multiple services
**Cause**: Railway infrastructure issue (rare)
**How to Check**:
- Check https://status.railway.app
- Check Railway Discord for reports

**Fix**: Wait for Railway to resolve, or contact support

## Diagnostic Steps

### Step 1: Verify Service is Running

In Railway Dashboard:
```
1. Navigate to your project
2. Select the Frontend service
3. Check the status indicator (top right):
   ✅ Green "Active" = Service is running
   ❌ Red "Crashed" = Service failed
   🟡 Yellow "Deploying" = Still deploying
```

### Step 2: Check Deployment Logs

Frontend Service:
```
1. Click on Frontend service
2. Go to "Deployments" tab
3. Click on latest deployment
4. Click "View Logs"
5. Look for:
   - "nginx: [emerg]" = Configuration error
   - Build success messages
   - "Server listening on port 80" or similar
```

Backend Service:
```
1. Click on Backend service
2. Go to "Deployments" tab
3. Click on latest deployment
4. Click "View Logs"
5. Look for:
   - Spring Boot ASCII art (successful startup)
   - "Started LabSignoffBackendApplication"
   - Any error messages about MongoDB, Redis, etc.
```

### Step 3: Verify Environment Variables

**Frontend Service Must Have**:
```bash
VITE_API_URL=https://[your-backend-url].up.railway.app
```

**Backend Service Must Have**:
```bash
SPRING_DATA_MONGODB_URI=mongodb+srv://...
SPRING_APPLICATION_NAME=lab-signoff
SERVER_PORT=8080
REDIS_HOST=${{Redis.REDIS_PRIVATE_URL}}
REDIS_PORT=6379
LTI_CLIENT_ID=your_client_id
CORS_ALLOWED_ORIGINS=https://[your-frontend-url].up.railway.app
```

Without these, the services will crash on startup.

### Step 4: Check Correct Domain

1. Railway Dashboard → Frontend Service → Settings → Domains
2. Copy the URL shown there (e.g., `frontend-production-abc123.up.railway.app`)
3. Use EXACTLY that URL in your browser

## Common Issues and Solutions

### Issue: "Build succeeded but service crashes immediately"

**Cause**: Missing environment variables

**Solution**:
1. Go to service → Variables tab
2. Add all required environment variables
3. Redeploy (Railway auto-deploys when you add variables)

### Issue: "Frontend loads but shows blank page"

**Cause**: Frontend can't connect to backend

**Solution**:
1. Check browser console (F12) for errors
2. Verify `VITE_API_URL` is set correctly
3. Verify backend CORS includes frontend URL
4. Check backend is running

### Issue: "Backend won't start - MongoDB connection error"

**Cause**: Invalid MongoDB connection string

**Solution**:
1. Test MongoDB URI locally first
2. Ensure connection string includes password
3. Check MongoDB Atlas whitelist (should allow all IPs: 0.0.0.0/0)
4. Verify database name in connection string

### Issue: "Backend won't start - Redis connection error"

**Cause**: Incorrect Redis configuration

**Solution**:
1. Verify Redis service is running in Railway
2. Use `${{Redis.REDIS_PRIVATE_URL}}` for internal connection
3. Don't hardcode Redis host/port

## Testing Deployment Status

### Test Backend API
```bash
# Replace with your actual backend URL
curl https://your-backend-url.up.railway.app/actuator/health
```

**Expected**: `{"status":"UP"}` or similar
**If fails**: Backend not running, check logs

### Test Frontend
```bash
# Replace with your actual frontend URL
curl -I https://your-frontend-url.up.railway.app
```

**Expected**: `HTTP/2 200` or `HTTP/1.1 200`
**If SSL error**: Service not running or cert not provisioned

## If Nothing Works

### Nuclear Option: Redeploy Everything

1. **Delete and recreate services**:
   - Railway Dashboard → Service → Settings → Delete Service
   - Create new service from GitHub repo
   - Set root directory and environment variables

2. **Check Railway Status**:
   - Visit https://status.railway.app
   - Check for ongoing incidents

3. **Contact Railway Support**:
   - Railway Discord: https://discord.gg/railway
   - Describe the SSL error with `.up.railway.app` domain
   - Share service ID and deployment logs

## Quick Checklist

Before asking for help, verify:
- [ ] Service shows "Active" (green) in Railway dashboard
- [ ] Latest deployment shows "Success"
- [ ] All required environment variables are set
- [ ] Using the exact URL shown in Railway → Domains
- [ ] Waited at least 5 minutes after first deployment
- [ ] Checked deployment logs for errors
- [ ] Railway status page shows no incidents

## Next Steps

1. **Check your Railway dashboard now**:
   - What is the service status? (Active/Crashed/Deploying)
   - What do the logs show?

2. **Verify the URL**:
   - Is `lab-signoff-app-development.up.railway.app` the correct URL?
   - Check Railway → Service → Settings → Domains

3. **Share details**:
   - Service status (green/red/yellow)
   - Last few lines of deployment logs
   - Environment variables that are set (don't share values, just names)

This will help identify the exact issue!
