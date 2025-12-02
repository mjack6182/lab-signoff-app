import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Header from '../../components/Header/Header'
import { api } from '../../config/api'
import { websocketService } from '../../services/websocketService'
import './lab-groups.css'

export default function LabGroups() {
  const { labId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [lab, setLab] = useState(null)
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [wsStatus, setWsStatus] = useState('DISCONNECTED')

  // Fetch lab and groups on mount
  useEffect(() => {
    Promise.all([
      fetch(api.labs()).then(res => res.json()),
      fetch(api.labGroups(labId)).then(res => {
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

  // WebSocket setup for live updates
  useEffect(() => {
    const onUpdate = (update) => {
      console.log('📡 WebSocket update received:', update)
      setGroups(prev =>
        prev.map(g =>
          g.groupId === update.groupId
            ? { ...g, status: update.status }
            : g
        )
      )
    }

    const onStatusChange = (status) => {
      setWsStatus(status)
    }

    websocketService.init()
    websocketService.addListener(onUpdate)
    websocketService.addStatusListener?.(onStatusChange)
    websocketService.subscribeToGroup('Group-1') // TEMP: change later

    return () => {
      websocketService.removeListener(onUpdate)
      websocketService.removeStatusListener?.(onStatusChange)
      websocketService.unsubscribeFromGroup('Group-1')

      // The missing cleanup
      websocketService.disconnect()
    }
  }, [])

  const handleGroupClick = (group) => {
    navigate(`/labs/${labId}/groups/${group.id}/checkpoints`)
  }

  const handleBackToLabs = () => {
    navigate('/class-selector')
  }

  if (loading) {
    return (
      <>
        <Header />
        <main className="lab-groups-shell">
          <div className="center-message">Loading groups...</div>
        </main>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="lab-groups-shell">
          <div className="error-message">
            <div>Error loading groups: {error}</div>
            <button onClick={handleBackToLabs}>Back to Classes</button>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Header />
      <main className="lab-groups-shell">
        <div
          className="page-header"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button className="back-btn" onClick={handleBackToLabs}>
              <span>←</span> Back to Classes
            </button>
            <div className="page-title-section">
              <h1 className="lab-title">{lab?.courseId || 'Lab'}</h1>
              <p className="lab-subtitle">Line Item: {lab?.lineItemId}</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span
              className={`font-semibold ${
                wsStatus === 'CONNECTED'
                  ? 'text-green-600'
                  : wsStatus === 'RECONNECTING'
                  ? 'text-orange-500'
                  : 'text-red-600'
              }`}
            >
              {wsStatus}
            </span>
            <button
              onClick={() =>
                fetch(api.wsTestBroadcast()).catch(console.error)
              }
              className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Test Broadcast
            </button>
          </div>
        </div>

        <section className="groups-section">
          <div className="section-header">
            <h2 className="section-title">Groups</h2>
            <span className="groups-count">
              {groups.length} group{groups.length !== 1 ? 's' : ''}
            </span>
          </div>

          <div className="groups-grid">
            {groups.length === 0 ? (
              <div className="no-groups">No groups available for this class</div>
            ) : (
              groups.map((group) => (
                <div
                  key={group.id}
                  className="group-card"
                  onClick={() => handleGroupClick(group)}
                >
                  <div className="group-card-header">
                    <h3 className="group-name">{group.groupId}</h3>
                    <span
                      className={`group-status status-${group.status
                        .toLowerCase()
                        .replace(' ', '-')}`}
                    >
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
                        {group.members.length} member
                        {group.members.length !== 1 ? 's' : ''}
                      </div>
                    </div>
                  </div>

                  <div className="group-card-footer">
                    <button className="view-btn">View Checkpoints</button>
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