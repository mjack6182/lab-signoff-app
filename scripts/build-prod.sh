#!/bin/bash

# Production build script for Lab Signoff Application
set -e

echo "🔨 Building Lab Signoff Application for production..."

echo "Building frontend..."
cd frontend
npm install
npm run build
cd ..

echo "Building backend..."
cd backend
mvn clean package -DskipTests
cd ..

echo "✅ Production build complete!"
echo ""
echo "Artifacts:"
echo "- Frontend: frontend/dist/"
echo "- Backend: backend/target/lab-signoff-backend-*.jar"