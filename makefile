# Load environment variables from .env file
ifneq (,$(wildcard .env))
    include .env
    export
endif

.PHONY: dev backend frontend stop install clean help

# Start both backend and frontend development servers concurrently
dev:
	@echo "Starting development servers..."
	@make -j2 backend frontend

# Start backend server
backend:
	@echo "Starting backend server..."
	@cd backend && ./gradlew bootRun

# Start frontend server
frontend:
	@echo "Starting frontend server..."
	@cd frontend && npm run dev

# Install dependencies for both frontend and backend
install:
	@echo "Installing backend dependencies..."
	@cd backend && ./gradlew build --no-daemon
	@echo "Installing frontend dependencies..."
	@cd frontend && npm install
	@echo "All dependencies installed."

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@cd backend && ./gradlew clean
	@cd frontend && rm -rf node_modules dist
	@echo "Clean complete."

# Stop all development servers (useful if running in background)
stop:
	@echo "Stopping development servers..."
	@pkill -f "gradlew bootRun" || true
	@pkill -f "vite" || true
	@echo "Servers stopped."

# Show available commands
help:
	@echo "Available commands:"
	@echo "  make dev      - Start both backend and frontend development servers"
	@echo "  make backend  - Start only the backend server"
	@echo "  make frontend - Start only the frontend server"
	@echo "  make install  - Install all dependencies"
	@echo "  make clean    - Clean build artifacts"
	@echo "  make stop     - Stop all development servers"
	@echo "  make help     - Show this help message"
