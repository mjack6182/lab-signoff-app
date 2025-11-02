import { createContext, useContext, useState, useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';

// Auth context for managing user authentication and role state with Auth0
const AuthContext = createContext(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const {
        user: auth0User,
        isAuthenticated,
        isLoading,
        loginWithRedirect,
        logout: auth0Logout,
        getAccessTokenSilently
    } = useAuth0();

    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Sync Auth0 user to our app user state and backend MongoDB
    useEffect(() => {
        const syncUser = async () => {
            if (isAuthenticated && auth0User) {
                try {
                    // Sync user to backend MongoDB
                    const response = await fetch('http://localhost:8080/api/auth/sync', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(auth0User)
                    });

                    const data = await response.json();

                    if (data.success) {
                        console.log('✅ User synced to MongoDB:', data.user);

                        // Extract role from Auth0 user metadata or claims
                        const roles = auth0User['https://lab-signoff-app/roles'] || ['Student'];
                        const primaryRole = Array.isArray(roles) ? roles[0] : roles;

                        const appUser = {
                            id: auth0User.sub,
                            name: auth0User.name,
                            email: auth0User.email,
                            picture: auth0User.picture,
                            role: primaryRole,
                            mongoId: data.user.id // MongoDB document ID
                        };

                        setUser(appUser);
                    } else {
                        console.error('❌ Failed to sync user to MongoDB:', data.error);
                        // Still set user from Auth0 data even if sync fails
                        const roles = auth0User['https://lab-signoff-app/roles'] || ['Student'];
                        const primaryRole = Array.isArray(roles) ? roles[0] : roles;

                        setUser({
                            id: auth0User.sub,
                            name: auth0User.name,
                            email: auth0User.email,
                            picture: auth0User.picture,
                            role: primaryRole
                        });
                    }
                } catch (error) {
                    console.error('❌ Error syncing user:', error);
                    // Still set user from Auth0 data even if sync fails
                    const roles = auth0User['https://lab-signoff-app/roles'] || ['Student'];
                    const primaryRole = Array.isArray(roles) ? roles[0] : roles;

                    setUser({
                        id: auth0User.sub,
                        name: auth0User.name,
                        email: auth0User.email,
                        picture: auth0User.picture,
                        role: primaryRole
                    });
                }
            } else {
                setUser(null);
            }
            setLoading(isLoading);
        };

        syncUser();
    }, [auth0User, isAuthenticated, isLoading]);

    const login = async () => {
        await loginWithRedirect();
    };

    const logout = () => {
        auth0Logout({
            logoutParams: {
                returnTo: window.location.origin
            }
        });
        setUser(null);
    };

    // Role switching is now only for demo/testing - in production, roles come from Auth0
    const switchRole = (newRole) => {
        if (user) {
            const updatedUser = { ...user, role: newRole };
            setUser(updatedUser);
            // Note: This only updates local state, not Auth0.
            // For production, roles should be managed in Auth0 dashboard
            console.warn('Role switching is for demo only. In production, manage roles in Auth0.');
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
        isAuthenticated,
        login,
        logout,
        switchRole,
        hasRole,
        hasAnyRole,
        isTeacher,
        isTA,
        isStudent,
        isTeacherOrTA,
        getAccessTokenSilently
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};