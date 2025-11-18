import { buildApiUrl } from '../config/api';

/**
 * Service for managing groups and enrollments
 */

/**
 * Fetch enrolled students for a class
 * @param {string} classId - The class ID
 * @param {boolean} activeOnly - Whether to fetch only active students (default: true)
 * @returns {Promise<Array>} Array of enrolled students with user details
 */
export const fetchEnrolledStudents = async (classId, activeOnly = true) => {
  const url = buildApiUrl(`/api/enrollments/class/${classId}/students?activeOnly=${activeOnly}`);
  const response = await fetch(url, { credentials: 'include' });

  if (!response.ok) {
    throw new Error(`Failed to fetch enrolled students: ${response.statusText}`);
  }

  return response.json();
};

/**
 * Fetch groups for a lab
 * @param {string} labId - The lab ID
 * @returns {Promise<Array>} Array of groups
 */
export const fetchLabGroups = async (labId) => {
  const url = buildApiUrl(`/lti/labs/${labId}/groups`);
  const response = await fetch(url, { credentials: 'include' });

  if (!response.ok) {
    throw new Error(`Failed to fetch lab groups: ${response.statusText}`);
  }

  return response.json();
};

/**
 * Randomize groups for a lab
 * @param {string} labId - The lab ID
 * @returns {Promise<Object>} Response with groups, groupCount, and message
 */
export const randomizeGroups = async (labId) => {
  const url = buildApiUrl(`/lti/labs/${labId}/randomize-groups`);
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to randomize groups: ${response.statusText}`);
  }

  return response.json();
};

/**
 * Update groups for a lab (bulk update)
 * @param {string} labId - The lab ID
 * @param {Array} groups - Array of group objects with members
 * @returns {Promise<Array>} Updated groups
 */
export const updateGroups = async (labId, groups) => {
  const url = buildApiUrl(`/lti/labs/${labId}/groups`);
  const response = await fetch(url, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(groups),
  });

  if (!response.ok) {
    throw new Error(`Failed to update groups: ${response.statusText}`);
  }

  return response.json();
};

/**
 * Calculate unassigned students (students not in any group)
 * @param {Array} enrolledStudents - Array of enrolled students
 * @param {Array} groups - Array of groups with members
 * @returns {Array} Students not assigned to any group
 */
export const calculateUnassignedStudents = (enrolledStudents, groups) => {
  const assignedUserIds = new Set();

  // Collect all user IDs that are in groups
  groups.forEach(group => {
    if (group.members && Array.isArray(group.members)) {
      group.members.forEach(member => {
        assignedUserIds.add(member.userId);
      });
    }
  });

  // Filter out students who are already in groups
  return enrolledStudents.filter(student => !assignedUserIds.has(student.userId));
};
