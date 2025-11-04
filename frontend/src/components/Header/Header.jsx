import { useState, useRef, useEffect } from 'react'
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
 * - Logout button (using Auth0)
 * - Fixed position at top of viewport
 */
export default function Header() {
  const { user, logout, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleSettings = () => {
    navigate('/settings')
    setDropdownOpen(false)
  }

  // Get display name - prefer firstName/lastName, fallback to name, then email
  const getDisplayName = () => {
    if (!user) return '';

    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }

    if (user.name && user.name !== user.email) {
      return user.name;
    }

    return user.email || 'User';
  }

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false)
      }
    }

    if (dropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [dropdownOpen])

  return (
    <header className="universal-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-name">Lab Signoff App</h1>
        </div>

        <div className="header-right">
          {isAuthenticated && user && (
            <div className="user-menu" ref={dropdownRef}>
              <button
                className="user-info-button"
                onClick={() => setDropdownOpen(!dropdownOpen)}
                aria-label="User menu"
              >
                {user.picture && (
                  <img
                    src={user.picture}
                    alt={getDisplayName()}
                    className="user-avatar"
                  />
                )}
                <div className="user-details">
                  <span className="user-name">{getDisplayName()}</span>
                  <span className="user-role">({user.role})</span>
                </div>
                <svg
                  className={`dropdown-icon ${dropdownOpen ? 'open' : ''}`}
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M4 6L8 10L12 6"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>

              {dropdownOpen && (
                <div className="dropdown-menu">
                  <button className="dropdown-item" onClick={handleSettings}>
                    <i className="fas fa-cog"></i>
                    Settings
                  </button>
                  <div className="dropdown-divider"></div>
                  <button className="dropdown-item logout" onClick={handleLogout}>
                    <i className="fas fa-sign-out-alt"></i>
                    Logout
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
