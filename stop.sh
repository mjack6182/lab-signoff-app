#!/bin/bash

echo "🛑 Stopping Lab Signoff App..."

# Kill any Spring Boot backend (Java processes running your main class)
BACKEND_PID=$(ps aux | grep "[j]ava.*LabSignoffBackendApplication" | awk '{print $2}')
if [ -n "$BACKEND_PID" ]; then
    echo "Stopping Spring Boot backend (PID: $BACKEND_PID)..."
    kill -9 $BACKEND_PID
else
    echo "No Spring Boot backend running."
fi

# Kill any Vite frontend process
VITE_PID=$(ps aux | grep "[v]ite" | awk '{print $2}')
if [ -n "$VITE_PID" ]; then
    echo "Stopping Vite frontend (PID: $VITE_PID)..."
    kill -9 $VITE_PID
else
    echo "No Vite frontend running."
fi

# Stop Nginx if running
if pgrep nginx >/dev/null; then
    echo "Stopping Nginx..."
    sudo nginx -s stop
else
    echo "Nginx not running."
fi

echo "✅ All servers stopped."
