import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './login.css';

export default function Login() {
    const { login, isAuthenticated, loading } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/class-selector');
        }
    }, [isAuthenticated, navigate]);

    const handleLogin = async () => {
        await login();
    };

    const handleStudentJoinClick = () => {
        navigate('/lab-join');
    };

    if (loading) {
        return (
            <div className="login-shell">
                <div className="loading-container">
                    <p>Loading...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="login-shell">
            <div className="login-container">
                {/* Left Column - App Information */}
                <div className="login-info">
                    <div className="logo-section">
                        <div className="app-logo">
                            <span className="logo-icon">✓</span>
                        </div>
                        <h1 className="app-title">Lab Signoff App</h1>
                        <p className="app-tagline">Streamline your lab management and student progress tracking</p>
                    </div>

                    <div className="features-section">
                        <h2 className="features-title">For Instructors</h2>
                        <ul className="features-list">
                            <li>
                                <span className="feature-icon">📋</span>
                                <div className="feature-content">
                                    <strong>Manage Labs</strong>
                                    <p>Create and organize lab assignments with checkpoints</p>
                                </div>
                            </li>
                            <li>
                                <span className="feature-icon">👥</span>
                                <div className="feature-content">
                                    <strong>Track Groups</strong>
                                    <p>Monitor student groups and their progress in real-time</p>
                                </div>
                            </li>
                            <li>
                                <span className="feature-icon">✅</span>
                                <div className="feature-content">
                                    <strong>Sign Off Checkpoints</strong>
                                    <p>Review and approve student work efficiently</p>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>

                {/* Right Column - Login Card */}
                <div className="login-form-wrapper">
                    <div className="login-card">
                        <h2 className="login-title">Teacher Portal</h2>
                        <p className="login-subtitle">Sign in to access the instructor dashboard</p>

                        <button
                            className="login-btn"
                            type="button"
                            onClick={handleLogin}
                            disabled={loading}
                        >
                            {loading ? 'Loading...' : 'Sign in with Auth0'}
                        </button>

                        <p className="auth-note">
                            Secure authentication powered by Auth0
                        </p>
                    </div>

                    {/* Student Notice */}
                    <div className="student-notice">
                        <div className="notice-icon">ℹ️</div>
                        <div className="notice-content">
                            <h3 className="notice-title">Are you a student?</h3>
                            <p className="notice-text">
                                You don't need to sign in. Join your lab using the join code provided by your instructor.
                            </p>
                            <button
                                className="student-join-btn"
                                type="button"
                                onClick={handleStudentJoinClick}
                            >
                                Enter Join Code
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
