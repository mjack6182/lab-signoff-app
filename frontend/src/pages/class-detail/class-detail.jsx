import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../../components/Header/Header'
import { api } from '../../config/api'
import { useAuth } from '../../contexts/AuthContext'
import './class-detail.css'

export default function ClassDetail() {
  const { classId } = useParams()
  const navigate = useNavigate()
  const { isTeacherOrTA } = useAuth()

  const [classInfo, setClassInfo] = useState(null)
  const [labs, setLabs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const canEdit = typeof isTeacherOrTA === 'function' ? isTeacherOrTA() : true

  const [isEditing, setIsEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editRosterText, setEditRosterText] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const loadClassData = () => {
    if (!classId) {
      return
    }

    setLoading(true)
    setError(null)

    Promise.all([
      fetch(api.classDetail(classId)).then((res) => {
        if (!res.ok) throw new Error('Failed to load class details')
        return res.json()
      }),
      fetch(api.classLabs(classId)).then((res) => {
        if (!res.ok) throw new Error('Failed to load labs for this class')
        return res.json()
      })
    ])
      .then(([classData, labsData]) => {
        setClassInfo(classData)
        setLabs(Array.isArray(labsData) ? labsData : [])
      })
      .catch((err) => {
        console.error('Error loading class detail:', err)
        setError(err.message || 'Unable to load class')
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadClassData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId])

  const handleLabClick = (lab) => {
    navigate(`/labs/${lab.id}/checkpoints`)
  }

  const startEditing = () => {
    if (!classInfo) return
    setEditName(classInfo.courseName || '')
    setEditRosterText((classInfo.roster || []).join('\n'))
    setSaveError(null)
    setIsEditing(true)
  }

  const cancelEditing = () => {
    setIsEditing(false)
    setSaveError(null)
    setSaving(false)
  }

  const handleSave = async () => {
    if (!classId) return

    const trimmedName = editName.trim()
    if (!trimmedName) {
      setSaveError('Class name cannot be empty.')
      return
    }

    const rosterList = editRosterText
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0)

    setSaving(true)
    setSaveError(null)

    try {
      const response = await fetch(api.classDetail(classId), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          courseName: trimmedName,
          roster: rosterList
        })
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.error || 'Failed to save changes')
      }

      const updated = await response.json()
      setClassInfo(updated)
      setIsEditing(false)
      setSaving(false)
      setSaveError(null)
    } catch (err) {
      console.error('Error updating class:', err)
      setSaveError(err instanceof Error ? err.message : 'Failed to save changes')
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <>
        <Header />
        <main className="class-detail-shell">
          <div className="class-detail-status">Loading class...</div>
        </main>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="class-detail-shell">
          <div className="class-detail-error">
            <div>{error}</div>
            <button
              className="class-detail-back"
              onClick={() => navigate('/class-selector')}
            >
              Back to Classes
            </button>
          </div>
        </main>
      </>
    )
  }

  if (!classInfo) {
    return (
      <>
        <Header />
        <main className="class-detail-shell">
          <div className="class-detail-status">Class not found.</div>
        </main>
      </>
    )
  }

  const {
    courseCode,
    courseName,
    section,
    term,
    archived,
    canvasMetadata
  } = classInfo
  const rosterEntries = Array.isArray(classInfo.roster) ? classInfo.roster : []

  const displayName = isEditing ? editName : (courseName || courseCode || 'Class')

  return (
    <>
      <Header />
      <main className="class-detail-shell">
        <div className="class-detail-header">
          <div className="class-detail-main">
            <button
              className="class-detail-back"
              onClick={() => navigate('/class-selector')}
            >
              Back to Classes
            </button>

            <div>
              {isEditing ? (
                <input
                  className="class-detail-input"
                  value={editName}
                  onChange={(event) => setEditName(event.target.value)}
                  placeholder="Class name"
                  disabled={saving}
                />
              ) : (
                <h1 className="class-detail-title">
                  {displayName}
                </h1>
              )}
              {!isEditing && (
                <p className="class-detail-subtitle">
                  {[
                    section ? `Section ${section}` : null,
                    term || null
                  ].filter(Boolean).join(' | ') || '\u00A0'}
                </p>
              )}
            </div>
          </div>

          <div className="class-detail-actions">
            <span className={`class-detail-status-badge ${archived ? 'archived' : 'active'}`}>
              {archived ? 'Archived' : 'Active'}
            </span>
            {canEdit && !isEditing && (
              <button
                type="button"
                className="class-detail-edit"
                onClick={startEditing}
              >
                Edit Details
              </button>
            )}
            {canEdit && isEditing && (
              <>
                <button
                  type="button"
                  className="class-detail-save"
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
                <button
                  type="button"
                  className="class-detail-cancel"
                  onClick={cancelEditing}
                  disabled={saving}
                >
                  Cancel
                </button>
              </>
            )}
          </div>
        </div>

        {canvasMetadata?.courseId && (
          <div className="class-detail-meta">
            Canvas Course ID: <strong>{canvasMetadata.courseId}</strong>
          </div>
        )}

        <div className="class-detail-content">
          <section className="class-detail-section">
            <div className="class-detail-section-header">
              <h2>Labs</h2>
              <span>{labs.length} lab{labs.length === 1 ? '' : 's'}</span>
            </div>

            {labs.length === 0 ? (
              <div className="class-detail-card-empty">
                No labs are associated with this class yet.
              </div>
            ) : (
              <div className="class-detail-labs">
                {labs.map((lab) => (
                  <div key={lab.id} className="class-detail-lab-card">
                    <div className="lab-card-main">
                      <h3>{lab.title || 'Untitled Lab'}</h3>
                      <p>{lab.description || 'No description provided.'}</p>

                      <div className="lab-card-meta">
                        <span>Checkpoints: {Array.isArray(lab.checkpoints) ? lab.checkpoints.length : lab.points}</span>
                        <span>Status: {lab.status}</span>
                        {lab.joinCode && <span>Join Code: {lab.joinCode}</span>}
                      </div>
                    </div>
                    <button onClick={() => handleLabClick(lab)}>
                      View Checkpoints
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="class-detail-section">
            <div className="class-detail-section-header">
              <h2>Roster</h2>
              <span>{rosterEntries.length} student{rosterEntries.length === 1 ? '' : 's'}</span>
            </div>

            {isEditing ? (
              <div className="class-detail-roster-edit">
                <textarea
                  className="class-detail-textarea"
                  value={editRosterText}
                  onChange={(event) => setEditRosterText(event.target.value)}
                  placeholder="Enter one student per line"
                  rows={10}
                  disabled={saving}
                />
                <p className="class-detail-roster-hint">
                  Blank lines are ignored. Enter one student name per line.
                </p>
                {saveError && (
                  <div className="class-detail-edit-error">{saveError}</div>
                )}
              </div>
            ) : (
              <>
                {rosterEntries.length === 0 ? (
                  <div className="class-detail-card-empty">
                    No students are currently in this class.
                  </div>
                ) : (
                  <ul className="class-detail-roster">
                    {rosterEntries.map((entry, index) => (
                      <li key={`${entry}-${index}`} className="class-detail-roster-item">
                        <span className="class-detail-roster-index">{index + 1}.</span>
                        <span className="class-detail-roster-value">
                          {entry || 'Unknown student'}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </>
            )}
          </section>
        </div>
      </main>
    </>
  )
}
