import { useNavigate } from 'react-router-dom'
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
 * - Logout button
 * - Fixed position at top of viewport
 */
export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="universal-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-name">Lab Signoff App</h1>
        </div>

        <div className="header-right">
          {user && (
            <>
              <div className="user-info">
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
