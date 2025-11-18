// API Configuration
// In production (Railway), VITE_API_URL will be set as a build arg
// In development, it defaults to localhost:8080
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Helper function to build API URLs
export const buildApiUrl = (path) => {
  // Ensure path starts with /
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
};

// Export specific endpoint builders for convenience
export const api = {
  labs: () => buildApiUrl('/lti/labs'),
  classes: (params) => {
    if (!params || Object.keys(params).length === 0) {
      return buildApiUrl('/api/classes');
    }
    const search = new URLSearchParams(params);
    return buildApiUrl(`/api/classes?${search.toString()}`);
  },
  classDetail: (classId) => buildApiUrl(`/api/classes/${classId}`),
  classLabs: (classId) => buildApiUrl(`/api/classes/${classId}/labs`),
  importClass: () => buildApiUrl('/api/classes/import'),
  labGroups: (labId) => buildApiUrl(`/lti/labs/${labId}/groups`),
  groups: () => buildApiUrl('/groups'),
  auth: {
    login: () => buildApiUrl('/api/auth/login'),
  },
  labs: {
    join: () => buildApiUrl('/api/labs/join'),
    byCode: (code) => buildApiUrl(`/api/labs/code/${code}`),
    roster: (labId) => buildApiUrl(`/api/labs/${labId}/roster`),
    selectStudent: (labId) => buildApiUrl(`/api/labs/${labId}/select-student`),
    selections: (labId) => buildApiUrl(`/api/labs/${labId}/selections`),
  },
  ws: () => buildApiUrl('/ws'),
};

export default API_BASE_URL;
