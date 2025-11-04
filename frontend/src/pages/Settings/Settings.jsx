import { useNavigate, Outlet, useLocation } from 'react-router-dom';
import Header from '../../components/Header/Header';
import SettingsSidebar from '../../components/SettingsSidebar/SettingsSidebar';
import UserSettings from './UserSettings';
import './Settings.css';

export default function Settings() {
    const navigate = useNavigate();
    const location = useLocation();

    // Check if we're on the root settings page
    const isRootPage = location.pathname === '/settings';

    return (
        <>
            <Header />
            <main className="settings-container">
                <div className="settings-content">
                    <header className="settings-header">
                        <div className="settings-header-content">
                            <button className="back-button" onClick={() => navigate('/lab-selector')} aria-label="Go back">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M12.5 15L7.5 10L12.5 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                </svg>
                                Back
                            </button>
                            <div>
                                <h1 className="settings-title">Settings</h1>
                                <p className="settings-subtitle">Manage your account and preferences</p>
                            </div>
                        </div>
                    </header>

                    <div className="settings-layout">
                        <SettingsSidebar />
                        <div className="settings-main">
                            {isRootPage ? <UserSettings /> : <Outlet />}
                        </div>
                    </div>
                </div>
            </main>
        </>
    );
}
