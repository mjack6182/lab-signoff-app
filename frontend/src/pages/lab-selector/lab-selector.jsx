import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Header from '../../components/Header/Header'
import './lab-selector.css';

/**
 * LabSelector Component
 *
 * Displays a list of all available labs fetched from the backend.
 * Each lab card shows the course ID and allows navigation to view groups for that lab.
 *
 * Backend Integration:
 * - Fetches labs from GET /lti/labs endpoint
 * - Displays lab information including courseId and lineItemId
 */
export default function LabSelector() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [labs, setLabs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Fetch labs from backend on component mount
  useEffect(() => {
    fetch('http://localhost:8080/lti/labs')
      .then((res) => {
        if (!res.ok) throw new Error('Failed to fetch labs')
        return res.json()
      })
      .then((data) => {
        setLabs(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch((err) => {
        console.error('Error fetching labs:', err)
        setError(err.message)
        setLoading(false)
      })
  }, [])

  const handleLabOpen = (lab) => {
    // Navigate to groups page for this lab
    navigate(`/labs/${lab.id}/groups`)
  }

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  if (loading) {
    return (
      <>
        <Header />
        <main className="lab-selector-shell">
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '50vh',
            fontSize: '18px',
            color: '#64748b'
          }}>
            Loading labs...
          </div>
        </main>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="lab-selector-shell">
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '50vh',
            fontSize: '18px',
            color: '#ef4444',
            flexDirection: 'column',
            gap: '12px'
          }}>
            <div>Error loading labs: {error}</div>
            <button
              onClick={() => window.location.reload()}
              style={{
                padding: '8px 16px',
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              Retry
            </button>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Header />
      <main className="lab-selector-shell">
        {/* Lab Grid */}
        <section className="labs-grid-section">
        <div className="labs-grid">
          {labs.length === 0 ? (
            <div style={{
              gridColumn: '1 / -1',
              textAlign: 'center',
              padding: '40px',
              color: '#64748b'
            }}>
              No labs available
            </div>
          ) : (
            labs.map(lab => (
              <div key={lab.id} className="lab-card">
                <div className="lab-card-header">
                  <h3 className="lab-card-title">
                    {lab.courseId}
                  </h3>
                  <span
                    className="lab-card-status"
                    style={{
                      backgroundColor: '#dcfce7',
                      color: '#166534',
                      borderColor: '#bbf7d0',
                      padding: '4px 12px',
                      borderRadius: '12px',
                      fontSize: '12px',
                      fontWeight: '500'
                    }}
                  >
                    Available
                  </span>
                </div>

                <div className="lab-card-body">
                  <p className="lab-card-course">Line Item: {lab.lineItemId}</p>
                  <p className="lab-card-description">Click to view groups for this lab</p>
                </div>

                <div className="lab-card-footer">
                  <button
                    className="lab-card-button open"
                    onClick={() => handleLabOpen(lab)}
                  >
                    View Groups
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </main>
    </>
  )
}