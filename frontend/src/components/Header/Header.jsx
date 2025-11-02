import { useAuth } from '../../contexts/AuthContext'
import './Header.css'

/**
 * Universal Header Component
 *
 * Fixed header displayed across all pages in the application.
 * Contains app branding and user information with logout functionality.
 *
 * Features:
 * - App name/branding
 * - Current user name and role
 * - Logout button (using Auth0)
 * - Fixed position at top of viewport
 */
export default function Header() {
  const { user, logout, isAuthenticated } = useAuth()

  const handleLogout = () => {
    logout()
  }

  return (
    <header className="universal-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-name">Lab Signoff App</h1>
        </div>

        <div className="header-right">
          {isAuthenticated && user && (
            <>
              <div className="user-info">
                {user.picture && (
                  <img
                    src={user.picture}
                    alt={user.name}
                    className="user-avatar"
                    style={{ width: '32px', height: '32px', borderRadius: '50%', marginRight: '0.5rem' }}
                  />
                )}
                <span className="user-name">{user.name}</span>
                <span className="user-role">({user.role})</span>
              </div>
              <button className="logout-button" onClick={handleLogout}>
                Logout
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
