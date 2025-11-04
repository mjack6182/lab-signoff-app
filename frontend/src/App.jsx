import { Routes, Route, NavLink, Navigate } from 'react-router-dom'
import Login from './pages/login/login'
import GroupList from './components/GroupList/GroupList'
import LabSelector from './pages/lab-selector/lab-selector'
import LabGroups from './pages/lab-groups/lab-groups'
import LabJoin from './pages/lab-join/lab-join.jsx'
import SelectStudent from './pages/select-student.jsx/select-student.jsx'
import StudentCheckpoints from './pages/student-checkpoints/checkpoints.jsx'
import Dashboard from './pages/dashboard/dashboard'
import CheckpointPage from './pages/checkpoints/checkpoints.jsx'
import Settings from './pages/Settings/Settings'
import ClassesSettings from './pages/Settings/ClassesSettings'
// import RoleDemo from './pages/role-demo/role-demo'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { StaffOnly } from './components/RoleGuard/RoleGuard'
import ProfileCompletionModal from './components/ProfileCompletionModal/ProfileCompletionModal'
// import RoleSwitcher from './components/RoleSwitcher/RoleSwitcher'

import { useEffect } from 'react';
import { createWebSocketClient } from "./services/websocketClient.js"; // ✅ import your WebSocket setup

function AppContent() {
    const { user, loading, isAuthenticated, hasCompletedProfile } = useAuth();

    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                backgroundColor: '#f6f8fc'
            }}>
                <div style={{ textAlign: 'center' }}>
                    <div style={{
                        fontSize: '24px',
                        marginBottom: '16px',
                        color: '#0f172a'
                    }}>
                        Lab Sign-Off App
                    </div>
                    <div style={{ color: '#64748b' }}>Loading...</div>
                </div>
            </div>
        );
    }

    // If not authenticated, show login page and student routes
    if (!isAuthenticated) {
        return (
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/lab-join" element={<LabJoin />} />
                <Route path="/select-student" element={<SelectStudent />} />
                <Route path="/student-checkpoints/:labId/:groupId" element={<StudentCheckpoints />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        );
    }

    // If authenticated, show all routes
    return (
        <>
            {/* Show profile completion modal if user hasn't completed profile */}
            <ProfileCompletionModal isOpen={isAuthenticated && !hasCompletedProfile()} />

            <Routes>
                <Route path="/" element={<Navigate to="/lab-selector" replace />} />
                <Route path="/login" element={<Navigate to="/lab-selector" replace />} />
                <Route path="/lab-join" element={<LabJoin />} />
                <Route path="/select-student" element={<SelectStudent />} />
                <Route path="/student-checkpoints/:labId/:groupId" element={<StudentCheckpoints />} />
                <Route path="/groups" element={<GroupList />} />
                <Route path="/lab-selector" element={<LabSelector />} />
                {/* Direct route from lab selector to checkpoints */}
                <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
                {/* Keep the old group-specific route for backward compatibility */}
                <Route path="/labs/:labId/groups/:groupId/checkpoints" element={<CheckpointPage />} />
                {/* Keep the groups page for potential admin functionality */}
                <Route path="/labs/:labId/groups" element={<LabGroups />} />
                <Route path="/settings" element={<Settings />}>
                    <Route path="classes" element={<ClassesSettings />} />
                </Route>
                <Route path="/dashboard" element={
                    user ? (
                        <StaffOnly fallback={<Navigate to="/lab-selector" replace />}>
                            <Dashboard />
                        </StaffOnly>
                    ) : <Navigate to="/login" replace />
                } />
                <Route path="/checkpoints" element={
                    user ? (
                        <StaffOnly fallback={<Navigate to="/lab-selector" replace />}>
                            <CheckpointPage />
                        </StaffOnly>
                    ) : <Navigate to="/login" replace />
                } />
                {/* <Route path="/role-demo" element={<RoleDemo />} /> */}

                {/* optional 404 */}
                <Route path="*" element={user ? <Navigate to="/lab-selector" replace /> : <Navigate to="/login" replace />} />
            </Routes>
        </>
    );
}

export default function App() {

    useEffect(() => {
        // 🟢 Create and connect the WebSocket client when the app starts
        const client = createWebSocketClient();
        client.activate();

        // 🔴 Clean up connection when the app is closed or refreshed
        return () => {
            client.deactivate();
        };
    }, []); // ← runs only once when the app loads


    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
}