/**
 * Mock Checkpoint Data
 * Used for development and testing of the student checkpoint view
 */

export const mockCheckpoints = [
    {
        id: 'checkpoint-1',
        name: 'Setup Environment',
        description: 'Install required dependencies and configure your development environment',
        points: 1,
        order: 1
    },
    {
        id: 'checkpoint-2',
        name: 'Implement Core Functionality',
        description: 'Complete the main feature implementation according to specifications',
        points: 1,
        order: 2
    },
    {
        id: 'checkpoint-3',
        name: 'Write Tests',
        description: 'Create unit tests with at least 80% code coverage',
        points: 1,
        order: 3
    },
    {
        id: 'checkpoint-4',
        name: 'Code Review',
        description: 'Submit code for review and address feedback',
        points: 1,
        order: 4
    },
    {
        id: 'checkpoint-5',
        name: 'Final Submission',
        description: 'Complete all requirements and submit final deliverables',
        points: 1,
        order: 5
    }
];

export default mockCheckpoints;
