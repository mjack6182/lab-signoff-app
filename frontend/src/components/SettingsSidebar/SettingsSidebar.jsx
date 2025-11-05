import { NavLink } from 'react-router-dom';
import './SettingsSidebar.css';

export default function SettingsSidebar() {
    return (
        <aside className="settings-sidebar">
            <nav className="sidebar-nav">
                <NavLink
                    to="/settings"
                    end
                    className={({ isActive }) =>
                        `sidebar-nav-item ${isActive ? 'active' : ''}`
                    }
                >
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M10 11C11.6569 11 13 9.65685 13 8C13 6.34315 11.6569 5 10 5C8.34315 5 7 6.34315 7 8C7 9.65685 8.34315 11 10 11Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M10 14C6.68629 14 4 15.7909 4 18H16C16 15.7909 13.3137 14 10 14Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                    <span>User Settings</span>
                </NavLink>

                <NavLink
                    to="/settings/classes"
                    className={({ isActive }) =>
                        `sidebar-nav-item ${isActive ? 'active' : ''}`
                    }
                >
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 6C3 5.44772 3.44772 5 4 5H16C16.5523 5 17 5.44772 17 6V14C17 14.5523 16.5523 15 16 15H4C3.44772 15 3 14.5523 3 14V6Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M10 5V2" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M7 9H13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M7 11H13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                    <span>Classes</span>
                </NavLink>
            </nav>
        </aside>
    );
}
