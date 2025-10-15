import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Header from '../../components/Header/Header'
import './lab-groups.css'

/**
 * LabGroups Component
 *
 * Displays all groups for a specific lab. Shows group information including
 * members and status. This is the second step in the user flow: Labs -> Groups -> Checkpoints
 *
 * Backend Integration:
 * - Fetches lab info from GET /lti/labs endpoint
 * - Fetches groups for the lab from GET /lti/labs/:labId/groups endpoint
 *
 * User Flow:
 * 1. User selects a lab from the lab selector
 * 2. This page displays all groups for that lab
 * 3. User can click on a group to view/manage checkpoints (future feature)
 */
export default function LabGroups() {
  const { labId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [lab, setLab] = useState(null)
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Fetch lab and groups data on component mount
  useEffect(() => {
    // Fetch lab information
    Promise.all([
      fetch(`http://localhost:8080/lti/labs`).then(res => res.json()),
      fetch(`http://localhost:8080/lti/labs/${labId}/groups`).then(res => {
        if (!res.ok) throw new Error('Failed to fetch groups')
        return res.json()
      })
    ])
      .then(([allLabs, groupsData]) => {
        const currentLab = allLabs.find(l => l.id === labId)
        setLab(currentLab)
        setGroups(Array.isArray(groupsData) ? groupsData : [])
        setLoading(false)
      })
      .catch((err) => {
        console.error('Error fetching data:', err)
        setError(err.message)
        setLoading(false)
      })
  }, [labId])

  const handleGroupClick = (group) => {
    // Navigate to checkpoint page for this group
    navigate(`/labs/${labId}/groups/${group.id}/checkpoints`)
  }

  const handleBackToLabs = () => {
    navigate('/lab-selector')
  }

  if (loading) {
    return (
      <>
        <Header />
        <main className="lab-groups-shell">
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '50vh',
            fontSize: '18px',
            color: '#64748b'
          }}>
            Loading groups...
          </div>
        </main>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="lab-groups-shell">
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
            <div>Error loading groups: {error}</div>
            <button
              onClick={handleBackToLabs}
              style={{
                padding: '8px 16px',
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              Back to Labs
            </button>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Header />
      <main className="lab-groups-shell">
        {/* Page Header with Back Button */}
        <div className="page-header">
          <button className="back-btn" onClick={handleBackToLabs}>
            <span>←</span> Back to Labs
          </button>
          <div className="page-title-section">
            <h1 className="lab-title">{lab?.courseId || 'Lab'}</h1>
            <p className="lab-subtitle">Line Item: {lab?.lineItemId}</p>
          </div>
        </div>

      {/* Groups Section */}
      <section className="groups-section">
        <div className="section-header">
          <h2 className="section-title">Groups</h2>
          <span className="groups-count">{groups.length} group{groups.length !== 1 ? 's' : ''}</span>
        </div>

        <div className="groups-grid">
          {groups.length === 0 ? (
            <div className="no-groups">
              No groups available for this lab
            </div>
          ) : (
            groups.map(group => (
              <div
                key={group.id}
                className="group-card"
                onClick={() => handleGroupClick(group)}
              >
                <div className="group-card-header">
                  <h3 className="group-name">{group.groupId}</h3>
                  <span className={`group-status status-${group.status.toLowerCase().replace(' ', '-')}`}>
                    {group.status}
                  </span>
                </div>

                <div className="group-card-body">
                  <div className="group-members">
                    <div className="members-label">Members:</div>
                    <div className="members-list">
                      {group.members.map((member, index) => (
                        <span key={index} className="member-item">
                          {member}
                        </span>
                      ))}
                    </div>
                    <div className="members-count">
                      {group.members.length} member{group.members.length !== 1 ? 's' : ''}
                    </div>
                  </div>
                </div>

                <div className="group-card-footer">
                  <button className="view-btn">
                    View Checkpoints
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
