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
  labGroups: (labId) => buildApiUrl(`/lti/labs/${labId}/groups`),
  groups: () => buildApiUrl('/groups'),
  auth: {
    login: () => buildApiUrl('/api/auth/login'),
  },
  ws: () => buildApiUrl('/ws'),
};

export default API_BASE_URL;
