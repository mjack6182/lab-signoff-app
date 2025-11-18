import { Routes, Route, NavLink, Navigate } from 'react-router-dom'
import Login from './pages/login/login'
import GroupList from './components/GroupList/GroupList'
import ClassSelector from './pages/class-selector/class-selector'
import ClassDetail from './pages/class-detail/class-detail'
import LabGroups from './pages/lab-groups/lab-groups'
import LabJoin from './pages/lab-join/lab-join.jsx'
import SelectStudent from './pages/select-student.jsx/select-student.jsx'
import StudentCheckpoints from './pages/student-checkpoints/checkpoints.jsx'
import Dashboard from './pages/dashboard/dashboard'
import CheckpointPage from './pages/checkpoints/checkpoints.jsx'
import Settings from './pages/Settings/Settings'
import ClassesSettings from './pages/Settings/ClassesSettings'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { StaffOnly } from './components/RoleGuard/RoleGuard'
import ProfileCompletionModal from './components/ProfileCompletionModal/ProfileCompletionModal'
import { useEffect } from 'react';

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

    // If not authenticated, show login + student routes
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

    return (
        <>
            <ProfileCompletionModal isOpen={isAuthenticated && !hasCompletedProfile()} />

            <Routes>
                <Route path="/" element={<Navigate to="/class-selector" replace />} />
                <Route path="/login" element={<Navigate to="/class-selector" replace />} />
                <Route path="/lab-join" element={<LabJoin />} />
                <Route path="/select-student" element={<SelectStudent />} />
                <Route path="/student-checkpoints/:labId/:groupId" element={<StudentCheckpoints />} />

                <Route path="/groups" element={<GroupList />} />
                <Route path="/class-selector" element={<ClassSelector />} />
                <Route path="/classes/:classId" element={<ClassDetail />} />

                <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
                <Route path="/labs/:labId/groups/:groupId/checkpoints" element={<CheckpointPage />} />

                <Route path="/labs/:labId/groups" element={<LabGroups />} />

                <Route path="/settings" element={<Settings />}>
                    <Route path="classes" element={<ClassesSettings />} />
                </Route>

                <Route path="/dashboard" element={
                    user ? (
                        <StaffOnly fallback={<Navigate to="/class-selector" replace />}>
                            <Dashboard />
                        </StaffOnly>
                    ) : <Navigate to="/login" replace />
                } />

                <Route path="/checkpoints" element={
                    user ? (
                        <StaffOnly fallback={<Navigate to="/class-selector" replace />}>
                            <CheckpointPage />
                        </StaffOnly>
                    ) : <Navigate to="/login" replace />
                } />

                <Route path="*" element={user ? <Navigate to="/class-selector" replace /> : <Navigate to="/login" replace />} />
            </Routes>
        </>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
}