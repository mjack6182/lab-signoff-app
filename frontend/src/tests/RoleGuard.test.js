/**
 * Test utilities for role-based access control
 * These tests verify that role guards work correctly and edge cases are handled
 */

import { render, screen } from '@testing-library/react';
import { AuthProvider } from '../contexts/AuthContext';
import { RoleGuard, TeacherOnly, TAOnly, StaffOnly, StudentOnly } from '../components/RoleGuard';

// Mock user data for testing
const mockUsers = {
    teacher: {
        id: 'teacher-1',
        name: 'Dr. Smith',
        email: 'smith@university.edu',
        role: 'Teacher'
    },
    ta: {
        id: 'ta-1',
        name: 'Jane Doe',
        email: 'jane@university.edu',
        role: 'TA'
    },
    student: {
        id: 'student-1',
        name: 'John Student',
        email: 'john@university.edu',
        role: 'Student'
    }
};

// Helper function to render components with auth context
const renderWithAuth = (component, user = null) => {
    // Mock localStorage for user data
    if (user) {
        window.localStorage.setItem('labSignoffUser', JSON.stringify(user));
    } else {
        window.localStorage.removeItem('labSignoffUser');
    }

    return render(
        <AuthProvider>
            {component}
        </AuthProvider>
    );
};

describe('RoleGuard Component', () => {
    beforeEach(() => {
        window.localStorage.clear();
    });

    test('renders content for authorized teacher', () => {
        renderWithAuth(
            <RoleGuard roles="Teacher">
                <div>Teacher Content</div>
            </RoleGuard>,
            mockUsers.teacher
        );
        
        expect(screen.getByText('Teacher Content')).toBeInTheDocument();
    });

    test('does not render content for unauthorized user', () => {
        renderWithAuth(
            <RoleGuard roles="Teacher">
                <div>Teacher Content</div>
            </RoleGuard>,
            mockUsers.student
        );
        
        expect(screen.queryByText('Teacher Content')).not.toBeInTheDocument();
    });

    test('renders fallback for unauthorized user when showFallback is true', () => {
        renderWithAuth(
            <RoleGuard 
                roles="Teacher" 
                fallback={<div>Access Denied</div>}
                showFallback={true}
            >
                <div>Teacher Content</div>
            </RoleGuard>,
            mockUsers.student
        );
        
        expect(screen.getByText('Access Denied')).toBeInTheDocument();
        expect(screen.queryByText('Teacher Content')).not.toBeInTheDocument();
    });

    test('works with multiple roles', () => {
        renderWithAuth(
            <RoleGuard roles={['Teacher', 'TA']}>
                <div>Staff Content</div>
            </RoleGuard>,
            mockUsers.ta
        );
        
        expect(screen.getByText('Staff Content')).toBeInTheDocument();
    });

    test('handles no user case', () => {
        renderWithAuth(
            <RoleGuard roles="Teacher">
                <div>Teacher Content</div>
            </RoleGuard>,
            null
        );
        
        expect(screen.queryByText('Teacher Content')).not.toBeInTheDocument();
    });
});

describe('Specific Role Guards', () => {
    beforeEach(() => {
        window.localStorage.clear();
    });

    test('TeacherOnly component works correctly', () => {
        renderWithAuth(
            <TeacherOnly>
                <div>Teacher Only Content</div>
            </TeacherOnly>,
            mockUsers.teacher
        );
        
        expect(screen.getByText('Teacher Only Content')).toBeInTheDocument();
    });

    test('TAOnly component works correctly', () => {
        renderWithAuth(
            <TAOnly>
                <div>TA Only Content</div>
            </TAOnly>,
            mockUsers.ta
        );
        
        expect(screen.getByText('TA Only Content')).toBeInTheDocument();
    });

    test('StaffOnly allows both Teacher and TA', () => {
        // Test with Teacher
        const { rerender } = renderWithAuth(
            <StaffOnly>
                <div>Staff Content</div>
            </StaffOnly>,
            mockUsers.teacher
        );
        
        expect(screen.getByText('Staff Content')).toBeInTheDocument();

        // Test with TA
        window.localStorage.setItem('labSignoffUser', JSON.stringify(mockUsers.ta));
        rerender(
            <AuthProvider>
                <StaffOnly>
                    <div>Staff Content</div>
                </StaffOnly>
            </AuthProvider>
        );
        
        expect(screen.getByText('Staff Content')).toBeInTheDocument();
    });

    test('StudentOnly component works correctly', () => {
        renderWithAuth(
            <StudentOnly>
                <div>Student Only Content</div>
            </StudentOnly>,
            mockUsers.student
        );
        
        expect(screen.getByText('Student Only Content')).toBeInTheDocument();
    });

    test('StaffOnly denies Student access', () => {
        renderWithAuth(
            <StaffOnly 
                fallback={<div>Staff Only</div>}
                showFallback={true}
            >
                <div>Staff Content</div>
            </StaffOnly>,
            mockUsers.student
        );
        
        expect(screen.getByText('Staff Only')).toBeInTheDocument();
        expect(screen.queryByText('Staff Content')).not.toBeInTheDocument();
    });
});

describe('Edge Cases', () => {
    beforeEach(() => {
        window.localStorage.clear();
    });

    test('handles invalid role gracefully', () => {
        const userWithInvalidRole = {
            ...mockUsers.teacher,
            role: 'InvalidRole'
        };

        renderWithAuth(
            <RoleGuard roles="Teacher">
                <div>Teacher Content</div>
            </RoleGuard>,
            userWithInvalidRole
        );
        
        expect(screen.queryByText('Teacher Content')).not.toBeInTheDocument();
    });

    test('handles missing role property', () => {
        const userWithoutRole = {
            id: 'user-1',
            name: 'User',
            email: 'user@test.com'
            // no role property
        };

        renderWithAuth(
            <RoleGuard roles="Teacher">
                <div>Teacher Content</div>
            </RoleGuard>,
            userWithoutRole
        );
        
        expect(screen.queryByText('Teacher Content')).not.toBeInTheDocument();
    });

    test('handles empty roles array', () => {
        renderWithAuth(
            <RoleGuard roles={[]}>
                <div>Content</div>
            </RoleGuard>,
            mockUsers.teacher
        );
        
        expect(screen.queryByText('Content')).not.toBeInTheDocument();
    });
});

// Performance test to ensure role checks don't slow down rendering
describe('Performance', () => {
    test('role checks complete quickly', () => {
        const start = performance.now();
        
        for (let i = 0; i < 1000; i++) {
            renderWithAuth(
                <RoleGuard roles="Teacher">
                    <div>Content {i}</div>
                </RoleGuard>,
                mockUsers.teacher
            );
        }
        
        const end = performance.now();
        const duration = end - start;
        
        // Should complete 1000 role checks in under 100ms
        expect(duration).toBeLessThan(100);
    });
});

export { mockUsers, renderWithAuth };