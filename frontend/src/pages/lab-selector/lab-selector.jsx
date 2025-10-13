import { useAuth } from '../../contexts/AuthContext'
import './lab-selector.css';

const labs = [
  {
    id: 'lab-01',
    title: 'Lab 1: Variables & Data Types',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 80,
    description: 'Basic variable declaration, primitive types, and type conversion',
    due: 'Sept 25, 2024',
  },
  {
    id: 'lab-02',
    title: 'Lab 2: Control Structures',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 35,
    description: 'If statements, loops, and conditional logic implementation',
    due: 'Oct 2, 2024',
  },
  {
    id: 'lab-03',
    title: 'Lab 3: Functions & Methods',
    course: 'CS 101: Introduction to Programming',
    status: 'upcoming',
    progress: 0,
    description: 'Function definition, parameters, return values, and scope',
    due: 'Oct 9, 2024',
  },
  {
    id: 'lab-04',
    title: 'Lab 4: Arrays & Collections',
    course: 'CS 101: Introduction to Programming',
    status: 'upcoming',
    progress: 0,
    description: 'Array manipulation, iteration, and basic data structures',
    due: 'Oct 16, 2024',
  },
  {
    id: 'lab-05',
    title: 'Lab 5: Object-Oriented Programming',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 60,
    description: 'Classes, objects, inheritance, and polymorphism',
    due: 'Oct 23, 2024',
  },
  {
    id: 'lab-06',
    title: 'Lab 6: Data Structures',
    course: 'CS 101: Introduction to Programming',
    status: 'closed',
    progress: 100,
    description: 'Lists, stacks, queues, and basic algorithms',
    due: 'Oct 30, 2024',
  },
]

const statusLabels = {
  open: 'Open',
  upcoming: 'Upcoming',
  closed: 'Closed',
}

const statusColors = {
  open: { bg: '#dcfce7', color: '#166534', border: '#bbf7d0' },
  upcoming: { bg: '#fef3c7', color: '#92400e', border: '#fde68a' },
  closed: { bg: '#f3f4f6', color: '#6b7280', border: '#d1d5db' },
}

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