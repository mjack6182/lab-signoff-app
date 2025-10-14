#!/bin/bash

echo "🚀 Starting Lab Signoff App..."

# Go to backend folder and start Spring Boot
cd backend
./gradlew bootRun &
BACK_PID=$!

# Go to frontend folder and start Vite
cd ../frontend
npm run dev &
FRONT_PID=$!

# Start or reload Nginx
sudo nginx -s reload || sudo nginx

echo ""
echo "✅ Backend running on port 8080"
echo "✅ Frontend running on port 5173"
echo "✅ Access the app at: http://localhost"
echo ""
echo "To stop servers later, run: ./stop.sh"

