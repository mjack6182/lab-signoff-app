import { createContext, useContext, useState, useEffect } from 'react';

// Auth context for managing user authentication and role state
const AuthContext = createContext(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Simulate authentication check on app load
    useEffect(() => {
        // In a real app, this would check for stored tokens, validate with server, etc.
        const checkAuth = async () => {
            try {
                // Simulate API call delay
                await new Promise(resolve => setTimeout(resolve, 500));

                // Check if user exists in localStorage
                const storedUser = localStorage.getItem('labSignoffUser');
                if (storedUser) {
                    setUser(JSON.parse(storedUser));
                } else {
                    // No stored user, redirect to login
                    setUser(null);
                }
            } catch (error) {
                console.error('Auth check failed:', error);
                setUser(null);
            } finally {
                setLoading(false);
            }
        };

        checkAuth();
    }, []);

    const login = async (credentials) => {
        try {
            // Simulate login API call
            const response = {
                id: 'u' + Math.random().toString(36).substr(2, 9),
                name: credentials.name || 'Demo User',
                email: credentials.email,
                role: credentials.role || 'Student'
            };

            setUser(response);
            localStorage.setItem('labSignoffUser', JSON.stringify(response));
            return response;
        } catch (error) {
            throw new Error('Login failed');
        }
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem('labSignoffUser');
    };

    const switchRole = (newRole) => {
        if (user) {
            const updatedUser = { ...user, role: newRole };
            setUser(updatedUser);
            localStorage.setItem('labSignoffUser', JSON.stringify(updatedUser));
        }
    };

    const hasRole = (role) => {
        return user?.role === role;
    };

    const hasAnyRole = (roles) => {
        return roles.includes(user?.role);
    };

    const isTeacher = () => hasRole('Teacher');
    const isTA = () => hasRole('TA');
    const isStudent = () => hasRole('Student');
    const isTeacherOrTA = () => hasAnyRole(['Teacher', 'TA']);

    const value = {
        user,
        loading,
        login,
        logout,
        switchRole,
        hasRole,
        hasAnyRole,
        isTeacher,
        isTA,
        isStudent,
        isTeacherOrTA
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};