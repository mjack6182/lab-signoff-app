// User types
export interface User {
  id: string
  email: string
  name: string
  role: 'student' | 'instructor' | 'admin'
  ltiUserId?: string
  createdAt: string
  updatedAt: string
}

// Lab types
export interface Lab {
  id: string
  title: string
  description: string
  courseId: string
  instructions: string
  dueDate?: string
  maxScore: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// Signoff types
export interface Signoff {
  id: string
  labId: string
  studentId: string
  instructorId?: string
  status: 'pending' | 'approved' | 'rejected' | 'needs_revision'
  score?: number
  feedback?: string
  submissionData: Record<string, any>
  submittedAt: string
  reviewedAt?: string
}

// Course types
export interface Course {
  id: string
  name: string
  code: string
  description?: string
  instructorIds: string[]
  studentIds: string[]
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// API Response types
export interface ApiResponse<T> {
  data: T
  message?: string
  status: 'success' | 'error'
}

export interface PaginatedResponse<T> {
  data: T[]
  pagination: {
    page: number
    limit: number
    total: number
    totalPages: number
  }
}

// WebSocket message types
export interface WebSocketMessage {
  type: 'signoff_update' | 'lab_assignment' | 'notification'
  payload: any
  timestamp: string
}