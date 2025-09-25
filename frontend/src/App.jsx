import { Routes, Route, NavLink, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import GroupList from './components/GroupList'

export default function App() {
    return (
        <>
            {/* Quick demo nav (remove later if embedding in Canvas) */}
            <nav style={{
                display:'flex', gap:12, padding:'10px 16px', borderBottom:'1px solid #e5e7eb',
                position:'sticky', top:0, background:'#fff', zIndex:10
            }}>
                <NavLink to="/login">Login</NavLink>
                <NavLink to="/groups">Groups</NavLink>
            </nav>

            <Routes>
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="/login" element={<Login />} />
                <Route path="/groups" element={<GroupList />} />
                {/* optional 404 */}
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </>
    )
}