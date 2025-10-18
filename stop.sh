#!/bin/bash

echo "🛑 Stopping Lab Signoff App..."

# ---------------------------
# Step 1: Stop frontend (Vite)
# ---------------------------
echo "⏹️  Stopping frontend (Vite)..."
pkill -f "vite" >/dev/null 2>&1

# ---------------------------
# Step 2: Stop backend (Spring Boot / Java)
# ---------------------------
echo "⏹️  Stopping backend (Spring Boot)..."
# Try normal stop first
pkill -f "java" >/dev/null 2>&1
sleep 2

# Force kill if still running on port 8080
if lsof -ti :8080 >/dev/null; then
  echo "⚠️  Backend still running on port 8080 — forcing kill..."
  kill -9 $(lsof -ti :8080) >/dev/null 2>&1
fi

# ---------------------------
# Step 3: Stop Nginx
# ---------------------------
echo "⏹️  Stopping Nginx..."
sudo pkill nginx >/dev/null 2>&1
sudo rm -f /opt/homebrew/var/run/nginx.pid
sleep 1

# Force kill if something still bound to port 80
if lsof -ti :80 >/dev/null; then
  echo "⚠️  Nginx or another process still using port 80 — forcing kill..."
  sudo kill -9 $(lsof -ti :80) >/dev/null 2>&1
fi

# ---------------------------
# Step 4: Confirm cleanup
# ---------------------------
if ! lsof -i :8080 >/dev/null && ! lsof -i :80 >/dev/null; then
  echo ""
  echo "✅ All servers stopped successfully."
else
  echo ""
  echo "❌ Warning: Some ports are still in use. Run these manually if needed:"
  echo "   sudo kill -9 \$(lsof -ti :8080)"
  echo "   sudo kill -9 \$(lsof -ti :80)"
fi