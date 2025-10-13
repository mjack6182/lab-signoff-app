import { useAuth } from '../../contexts/AuthContext';

/**
 * Role Switcher component for development/demo purposes
 * Allows switching between different user roles to test role-based functionality
 */
export default function RoleSwitcher() {
    const { user, switchRole, isTeacher, isTA, isStudent } = useAuth();

    if (!user) return null;

    const roles = [
        { value: 'Teacher', label: 'Teacher', color: '#059669' },
        { value: 'TA', label: 'Teaching Assistant', color: '#3b82f6' },
        { value: 'Student', label: 'Student', color: '#64748b' }
    ];

    return (
        <div className="role-switcher">
            <div className="role-switcher-header">
                <span className="role-switcher-label">Current Role:</span>
                <span 
                    className="role-switcher-current"
                    style={{ 
                        color: roles.find(r => r.value === user.role)?.color || '#64748b' 
                    }}
                >
                    {user.role}
                </span>
            </div>
            
            <div className="role-switcher-buttons">
                {roles.map(role => (
                    <button
                        key={role.value}
                        onClick={() => switchRole(role.value)}
                        className={`role-switch-btn ${user.role === role.value ? 'active' : ''}`}
                        style={{
                            borderColor: role.color,
                            backgroundColor: user.role === role.value ? role.color : 'transparent',
                            color: user.role === role.value ? '#fff' : role.color
                        }}
                        title={`Switch to ${role.label} role`}
                    >
                        {role.label}
                    </button>
                ))}
            </div>
            
            <div className="role-switcher-info">
                <small>Demo: Switch roles to test different access levels</small>
            </div>
        </div>
    );
}