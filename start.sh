#!/bin/bash

echo "🚀 Starting Lab Signoff App..."

# ---------------------------
# Step 1: Kill old processes
# ---------------------------
echo "🧹 Cleaning up old processes..."
pkill -f "vite" >/dev/null 2>&1
pkill -f "java" >/dev/null 2>&1
sudo pkill nginx >/dev/null 2>&1
sudo rm -f /opt/homebrew/var/run/nginx.pid

# ---------------------------
# Step 2: Build frontend
# ---------------------------
echo "🛠 Building frontend..."
cd "$(dirname "$0")/frontend" || exit 1
npm install >/dev/null 2>&1
npm run build

# ---------------------------
# Step 3: Start Nginx (foreground)
# ---------------------------
echo "🌐 Starting Nginx..."
sudo nginx -g "daemon off;" &
NGINX_PID=$!


# ---------------------------
# Step 4: Start backend (foreground)
# ---------------------------
echo "🚀 Starting backend..."
cd ../backend || exit 1
./gradlew bootRun


# ---------------------------
# Step 5: Cleanup on exit
# ---------------------------
echo "🛑 Stopping Nginx..."
sudo kill $NGINX_PID >/dev/null 2>&1