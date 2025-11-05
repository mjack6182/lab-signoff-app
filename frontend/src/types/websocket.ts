/**
 * WebSocket message types for real-time updates
 * These types must match the backend DTOs
 */

export type CheckpointUpdateStatus = 'PASS' | 'RETURN';

export type GroupStatus = 'FORMING' | 'IN_PROGRESS' | 'COMPLETED' | 'SIGNED_OFF';

export type HelpQueueStatus = 'WAITING' | 'CLAIMED' | 'RESOLVED' | 'CANCELLED';

export type HelpQueuePriority = 'NORMAL' | 'URGENT';

/**
 * Checkpoint update event - sent when a checkpoint is signed off or returned
 */
export interface CheckpointUpdate {
  labId: string;
  groupId: string;
  checkpointNumber: number;
  status: CheckpointUpdateStatus;
  signedOffBy?: string;
  signedOffByName?: string;
  timestamp: string; // ISO 8601 format
  notes?: string;
  pointsAwarded?: number;
}

/**
 * Group status update event - sent when a group's overall status changes
 */
export interface GroupStatusUpdate {
  labId: string;
  groupId: string;
  status: GroupStatus;
  previousStatus?: GroupStatus;
  timestamp: string;
  performedBy?: string;
  performedByName?: string;
  totalScore?: number;
  finalGrade?: string;
}

/**
 * Help queue update event - sent when help requests are raised, claimed, or resolved
 */
export interface HelpQueueUpdate {
  id: string;
  labId: string;
  groupId: string;
  status: HelpQueueStatus;
  previousStatus?: HelpQueueStatus;
  priority: HelpQueuePriority;
  position?: number;
  requestedBy: string;
  requestedByName?: string;
  claimedBy?: string;
  claimedByName?: string;
  timestamp: string;
  description?: string;
}

/**
 * WebSocket topics for subscriptions
 */
export const WebSocketTopics = {
  // Lab-specific topics
  labCheckpoints: (labId: string) => `/topic/labs/${labId}/checkpoints`,
  labGroups: (labId: string) => `/topic/labs/${labId}/groups`,
  labHelpQueue: (labId: string) => `/topic/labs/${labId}/help-queue`,

  // Group-specific topics
  groupCheckpoints: (groupId: string) => `/topic/groups/${groupId}/checkpoints`,
  groupStatus: (groupId: string) => `/topic/groups/${groupId}/status`,
  groupHelpQueue: (groupId: string) => `/topic/groups/${groupId}/help-queue`,

  // Legacy topic (for backward compatibility)
  groupUpdates: '/topic/group-updates',

  // Test topic
  greetings: '/topic/greetings',
} as const;
