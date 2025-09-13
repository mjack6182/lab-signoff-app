#!/bin/bash

# Development setup script for Lab Signoff Application
set -e

echo "🚀 Setting up Lab Signoff Application development environment..."

# Check for required tools
check_tool() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ $1 is not installed. Please install it first."
        exit 1
    else
        echo "✅ $1 is available"
    fi
}

echo "Checking required tools..."
check_tool "java"
check_tool "mvn" 
check_tool "node"
check_tool "npm"
check_tool "docker"
check_tool "docker-compose"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "21" ]; then
    echo "❌ Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
else
    echo "✅ Java version: $JAVA_VERSION"
fi

# Check Node version  
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt "20" ]; then
    echo "❌ Node.js 20 or higher is required. Current version: $NODE_VERSION"
    exit 1
else
    echo "✅ Node.js version: $(node -v)"
fi

echo "Starting infrastructure..."
cd infra
docker-compose up -d
cd ..

echo "Installing frontend dependencies..."
cd frontend
npm install
cd ..

echo "Installing backend dependencies..."
cd backend
mvn clean compile
cd ..

echo "✅ Development environment setup complete!"
echo ""
echo "Next steps:"
echo "1. Start the backend: cd backend && mvn spring-boot:run"
echo "2. Start the frontend: cd frontend && npm run dev"
echo "3. Visit http://localhost:3000 for the frontend"
echo "4. Visit http://localhost:8080 for the backend API"
echo "5. Visit http://localhost:8081 for MongoDB admin interface"