import { Routes, Route, NavLink, Navigate } from 'react-router-dom'
import Login from './pages/login/login'
import GroupList from './components/GroupList/GroupList'
import LabSelector from './pages/lab-selector/lab-selector'
import Dashboard from './pages/dashboard/dashboard'
import CheckpointPage from './pages/checkpoints/checkpoint'
import RoleDemo from './pages/role-demo/role-demo'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { StaffOnly } from './components/RoleGuard/RoleGuard'
import RoleSwitcher from './components/RoleSwitcher/RoleSwitcher'

function AppContent() {
    const { user, loading } = useAuth();
    
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

    return (
        <>
            {/* Quick demo nav (remove later if embedding in Canvas) */}
            <nav style={{
                display:'flex', gap:12, padding:'10px 16px', borderBottom:'1px solid #e5e7eb',
                position:'sticky', top:0, background:'#fff', zIndex:10, alignItems: 'center'
            }}>
                <div style={{ display: 'flex', gap: 12, flex: 1 }}>
                    <NavLink to="/login">Login</NavLink>
                    <NavLink to="/groups">Groups</NavLink>
                    <NavLink to="/lab-selector">Lab Selector</NavLink>
                    <StaffOnly>
                        <NavLink to="/dashboard">Dashboard</NavLink>
                        <NavLink to="/checkpoints">Checkpoints</NavLink>
                    </StaffOnly>
                    <NavLink to="/role-demo">Role Demo</NavLink>
                </div>
                
                {user && (
                    <div style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: 12,
                        fontSize: '14px',
                        color: '#64748b'
                    }}>
                        <span>
                            {user.name} ({user.role})
                        </span>
                    </div>
                )}
            </nav>

            {/* Role Switcher for Demo */}
            {user && (
                <div style={{ 
                    position: 'fixed', 
                    top: 60, 
                    right: 16, 
                    zIndex: 100,
                    width: 250
                }}>
                    <RoleSwitcher />
                </div>
            )}

            <Routes>
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="/login" element={<Login />} />
                <Route path="/groups" element={<GroupList />} />
                <Route path="/lab-selector" element={<LabSelector />} />
                <Route path="/dashboard" element={
                    <StaffOnly fallback={<Navigate to="/groups" replace />}>
                        <Dashboard />
                    </StaffOnly>
                } />
                <Route path="/checkpoints" element={
                    <StaffOnly fallback={<Navigate to="/groups" replace />}>
                        <CheckpointPage />
                    </StaffOnly>
                } />
                <Route path="/role-demo" element={<RoleDemo />} />

                {/* optional 404 */}
                <Route path="*" element={<Navigate to="/login" replace />} />
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