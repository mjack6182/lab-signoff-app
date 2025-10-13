import { useAuth } from '../../contexts/AuthContext'
import { labs, statusLabels, statusColors } from '../../mock/labs'
import './lab-selector.css';

export default function LabSelector() {
  const { user, logout } = useAuth()

  const handleLabOpen = (lab) => {
    if (lab.status === 'open') {
      // Navigate to checkpoints page for this lab
      window.location.href = '/checkpoints'
    }
  }

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  return (
    <main className="lab-selector-shell">
      {/* Header */}
      <header className="lab-selector-header">
        <div className="header-left">
          <h1 className="app-title">Lab-Signoff-App</h1>
        </div>
        <div className="header-right">
          {user && (
            <>
              <div className="user-profile">
                <span className="user-name">{user.name}</span>
                <span className="user-role">({user.role})</span>
              </div>
              <button className="logout-btn" onClick={handleLogout}>
                Logout
              </button>
            </>
          )}
        </div>
      </header>

      {/* Lab Grid */}
      <section className="labs-grid-section">
        <div className="labs-grid">
          {labs.map(lab => {
            const statusStyle = statusColors[lab.status]
            const isOpen = lab.status === 'open'
            
            return (
              <div key={lab.id} className="lab-card">
                <div className="lab-card-header">
                  <h3 className="lab-card-title">
                    {lab.title}
                  </h3>
                  <span 
                    className="lab-card-status"
                    style={{
                      backgroundColor: statusStyle.bg,
                      color: statusStyle.color,
                      borderColor: statusStyle.border,
                    }}
                  >
                    {statusLabels[lab.status]}
                  </span>
                </div>
                
                <div className="lab-card-body">
                  <p className="lab-card-course">{lab.course}</p>
                  <p className="lab-card-description">{lab.description}</p>
                  
                  <div className="lab-card-progress">
                    <div className="progress-header">
                      <span className="progress-label">Progress</span>
                      <span className="progress-value">{lab.progress}%</span>
                    </div>
                    <div className="progress-bar">
                      <div 
                        className="progress-fill"
                        style={{ width: `${lab.progress}%` }}
                      ></div>
                    </div>
                  </div>
                </div>
                
                <div className="lab-card-footer">
                  <button 
                    className={`lab-card-button ${isOpen ? 'open' : 'disabled'}`}
                    onClick={() => handleLabOpen(lab)}
                    disabled={!isOpen}
                  >
                    {isOpen ? 'Open' : statusLabels[lab.status]}
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      </section>
    </main>
  )
}