import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import './Settings.css';

export default function ClassesSettings() {
    const { user } = useAuth();
    const [classes, setClasses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Only teachers and TAs can manage classes
    const canManageClasses = user?.role === 'Teacher' || user?.role === 'TA';

    useEffect(() => {
        if (canManageClasses) {
            // TODO: Fetch classes from backend
            // For now, we'll show a placeholder
            setLoading(false);
        } else {
            setLoading(false);
        }
    }, [canManageClasses]);

    if (!canManageClasses) {
        return (
            <div className="settings-card">
                <div className="card-section">
                    <h2 className="section-title">Classes</h2>
                    <p className="section-description">Manage your class rosters</p>
                </div>

                <div style={{ padding: '24px' }}>
                    <div className="alert alert-error">
                        You do not have permission to manage classes. This feature is only available for Teachers and TAs.
                    </div>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="settings-card">
                <div className="card-section">
                    <h2 className="section-title">Classes</h2>
                    <p className="section-description">Manage your class rosters</p>
                </div>

                <div style={{ padding: '24px', textAlign: 'center', color: '#64748b' }}>
                    Loading classes...
                </div>
            </div>
        );
    }

    return (
        <div className="settings-card">
            <div className="card-section">
                <h2 className="section-title">Classes</h2>
                <p className="section-description">Manage your class rosters and student enrollments</p>
            </div>

            <div style={{ padding: '24px' }}>
                <div className="classes-empty-state">
                    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ margin: '0 auto 16px', display: 'block', color: '#cbd5e1' }}>
                        <path d="M12 24C12 22.8954 12.8954 22 14 22H50C51.1046 22 52 22.8954 52 24V44C52 45.1046 51.1046 46 50 46H14C12.8954 46 12 45.1046 12 44V24Z" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M32 22V14" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M22 30H42" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M22 38H42" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                    <h3 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: 600, color: '#0f172a', textAlign: 'center' }}>
                        Class Management Coming Soon
                    </h3>
                    <p style={{ margin: '0 0 24px 0', fontSize: '14px', color: '#64748b', textAlign: 'center', maxWidth: '400px', marginLeft: 'auto', marginRight: 'auto' }}>
                        This feature will allow you to create and manage classes, add or remove students, and organize your course rosters all in one place.
                    </p>
                    <div style={{ textAlign: 'center' }}>
                        <button className="btn btn-primary" disabled style={{ cursor: 'not-allowed' }}>
                            Create Class (Coming Soon)
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
