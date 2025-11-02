import { useAuth } from '../../contexts/AuthContext';

/**
 * Role-based guard component that conditionally renders children based on user roles
 * 
 * @param {Object} props
 * @param {string|string[]} props.roles - Required role(s) to access content
 * @param {React.ReactNode} props.children - Content to render if user has permission
 * @param {React.ReactNode} props.fallback - Content to render if user lacks permission
 * @param {boolean} props.showFallback - Whether to show fallback or hide entirely
 * @returns {React.ReactNode}
 */
export const RoleGuard = ({ 
    roles, 
    children, 
    fallback = null, 
    showFallback = false 
}) => {
    const { user, hasRole, hasAnyRole } = useAuth();

    if (!user) {
        return showFallback ? fallback : null;
    }

    const allowedRoles = Array.isArray(roles) ? roles : [roles];
    const hasPermission = hasAnyRole(allowedRoles);

    if (!hasPermission) {
        return showFallback ? fallback : null;
    }

    return children;
};

/**
 * Guard for teacher-only content
 */
export const TeacherOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard 
        roles="Teacher" 
        fallback={fallback} 
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Guard for TA-only content
 */
export const TAOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard 
        roles="TA" 
        fallback={fallback} 
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Guard for Teacher or TA content (staff only)
 */
export const StaffOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard
        roles={['Teacher', 'TA']}
        fallback={fallback}
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Guard for Admin-only content
 */
export const AdminOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard
        roles="Admin"
        fallback={fallback}
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Guard for staff or admin content (Teacher, TA, or Admin)
 */
export const StaffOrAdminOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard
        roles={['Teacher', 'TA', 'Admin']}
        fallback={fallback}
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Guard for student-only content
 */
export const StudentOnly = ({ children, fallback, showFallback = false }) => (
    <RoleGuard
        roles="Student"
        fallback={fallback}
        showFallback={showFallback}
    >
        {children}
    </RoleGuard>
);

/**
 * Hook for conditional rendering based on roles
 */
export const useRoleGuard = () => {
    const { hasRole, hasAnyRole, isTeacher, isTA, isStudent, isTeacherOrTA, isAdmin, isStaffOrAdmin } = useAuth();

    return {
        hasRole,
        hasAnyRole,
        isTeacher,
        isTA,
        isStudent,
        isAdmin,
        isTeacherOrTA,
        isStaffOrAdmin,
        canAccessTeacherFeatures: isTeacher,
        canAccessTAFeatures: isTA,
        canAccessStaffFeatures: isTeacherOrTA,
        canAccessAdminFeatures: isAdmin,
        canAccessStaffOrAdminFeatures: isStaffOrAdmin,
        canAccessStudentFeatures: isStudent
    };
};