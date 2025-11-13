import { buildApiUrl, api } from '../config/api.js';

// API Service for handling backend requests
class ApiService {
    constructor() {
        this.baseUrl = buildApiUrl('');
    }

    // Generic request method
    async request(url, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            ...options,
        };

        try {
            const response = await fetch(url, config);

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            // Handle empty responses
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error('API Request failed:', error);
            throw error;
        }
    }

    // Join lab with code and get roster
    async joinLabWithCode(labCode) {
        const response = await this.request(api.labs.join(), {
            method: 'POST',
            body: JSON.stringify({ labCode: labCode.toUpperCase() }),
        });

        return response;
    }

    // Get lab roster by lab ID
    async getLabRoster(labId) {
        const response = await this.request(api.labs.roster(labId), {
            method: 'GET',
        });

        return response;
    }

    // Get lab details by code
    async getLabByCode(labCode) {
        const response = await this.request(api.labs.byCode(labCode.toUpperCase()), {
            method: 'GET',
        });

        return response;
    }

    // Submit student selection
    async selectStudent(labId, studentName, browserFingerprint) {
        const response = await this.request(api.labs.selectStudent(labId), {
            method: 'POST',
            body: JSON.stringify({
                studentName,
                browserFingerprint,
                timestamp: new Date().toISOString()
            }),
        });

        return response;
    }

    // Get student selections for a lab (for validation)
    async getStudentSelections(labId) {
        const response = await this.request(api.labs.selections(labId), {
            method: 'GET',
        });

        return response;
    }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;