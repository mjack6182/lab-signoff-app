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
                    const response = await fetch(`${import.meta.env.VITE_API_URL}/api/auth/sync`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(auth0User)
                    });

                    const data = await response.json();

                    if (data.success) {
                        console.log('✅ User synced to MongoDB:', data.user);

                        // Extract role from MongoDB backend (source of truth)
                        // Fallback to Auth0 claims if backend doesn't have role
                        const backendRole = data.user.primaryRole || data.user.role;
                        const auth0Roles = auth0User['https://lab-signoff-app/roles'] || ['Teacher'];
                        const auth0PrimaryRole = Array.isArray(auth0Roles) ? auth0Roles[0] : auth0Roles;

                        // Prefer backend role over Auth0 role
                        const primaryRole = backendRole || auth0PrimaryRole;

                        // Extract firstName and lastName from backend (prioritize backend over Auth0)
                        // Only use Auth0 data if backend explicitly has null/undefined
                        const firstName = data.user.firstName !== null && data.user.firstName !== undefined
                            ? data.user.firstName
                            : (auth0User.given_name || null);
                        const lastName = data.user.lastName !== null && data.user.lastName !== undefined
                            ? data.user.lastName
                            : (auth0User.family_name || null);

                        const appUser = {
                            id: auth0User.sub,
                            name: auth0User.name,
                            firstName: firstName,
                            lastName: lastName,
                            email: auth0User.email,
                            picture: auth0User.picture,
                            role: primaryRole,
                            mongoId: data.user.id // MongoDB document ID
                        };

                        console.log('👤 User profile loaded:', {
                            firstName: firstName,
                            lastName: lastName,
                            hasFirstName: firstName !== null && firstName !== undefined && firstName !== '',
                            hasLastName: lastName !== null && lastName !== undefined && lastName !== '',
                            role: primaryRole,
                            backendRole: data.user.primaryRole || data.user.role,
                            auth0Role: auth0PrimaryRole
                        });

                        setUser(appUser);
                    } else {
                        console.error('❌ Failed to sync user to MongoDB:', data.error);
                        // Still set user from Auth0 data even if sync fails
                        const roles = auth0User['https://lab-signoff-app/roles'] || ['Teacher'];
                        const primaryRole = Array.isArray(roles) ? roles[0] : roles;

                        setUser({
                            id: auth0User.sub,
                            name: auth0User.name,
                            firstName: auth0User.given_name || null,
                            lastName: auth0User.family_name || null,
                            email: auth0User.email,
                            picture: auth0User.picture,
                            role: primaryRole
                        });
                    }
                } catch (error) {
                    console.error('❌ Error syncing user:', error);
                    // Still set user from Auth0 data even if sync fails
                    const roles = auth0User['https://lab-signoff-app/roles'] || ['Teacher'];
                    const primaryRole = Array.isArray(roles) ? roles[0] : roles;

                    setUser({
                        id: auth0User.sub,
                        name: auth0User.name,
                        firstName: auth0User.given_name || null,
                        lastName: auth0User.family_name || null,
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
    const isAdmin = () => hasRole('Admin');
    const isTeacherOrTA = () => hasAnyRole(['Teacher', 'TA']);
    const isStaffOrAdmin = () => hasAnyRole(['Teacher', 'TA', 'Admin']);

    // Check if user has completed their profile (has first and last name)
    const hasCompletedProfile = () => {
        if (!user) return false;

        // Check if firstName and lastName exist and are not null/undefined/empty
        const hasFirstName = user.firstName !== null &&
                            user.firstName !== undefined &&
                            typeof user.firstName === 'string' &&
                            user.firstName.trim() !== '';

        const hasLastName = user.lastName !== null &&
                           user.lastName !== undefined &&
                           typeof user.lastName === 'string' &&
                           user.lastName.trim() !== '';

        return hasFirstName && hasLastName;
    };

    // Update user profile (firstName and lastName)
    const updateUserProfile = async (firstName, lastName) => {
        if (!user) {
            throw new Error('No user logged in');
        }

        try {
            const response = await fetch('http://localhost:8080/api/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    auth0Id: user.id,
                    firstName: firstName,
                    lastName: lastName
                })
            });

            const data = await response.json();

            if (data.success) {
                // Update local user state
                const updatedUser = {
                    ...user,
                    firstName: data.user.firstName,
                    lastName: data.user.lastName,
                    name: data.user.name
                };
                setUser(updatedUser);
                return data.user;
            } else {
                throw new Error(data.error || 'Failed to update profile');
            }
        } catch (error) {
            console.error('❌ Error updating profile:', error);
            throw error;
        }
    };

    // Delete user account
    const deleteAccount = async () => {
        if (!user) {
            throw new Error('No user logged in');
        }

        try {
            const response = await fetch('http://localhost:8080/api/users/account', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    auth0Id: user.id
                })
            });

            const data = await response.json();

            if (data.success) {
                // Log out the user after successful deletion
                console.log('✅ Account deleted successfully');
                logout();
            } else {
                throw new Error(data.error || 'Failed to delete account');
            }
        } catch (error) {
            console.error('❌ Error deleting account:', error);
            throw error;
        }
    };

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
        isAdmin,
        isTeacherOrTA,
        isStaffOrAdmin,
        hasCompletedProfile,
        updateUserProfile,
        deleteAccount,
        getAccessTokenSilently
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};