import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import Header from '../../components/Header/Header'
import { api } from '../../config/api'
import { useAuth } from '../../contexts/AuthContext'
import './class-selector.css';

/**
 * ClassSelector Component
 *
 * Displays a list of all available classes fetched from the backend.
 * Each card shows key class details and lets staff jump straight into checkpoints.
 * This is the first step in the simplified user flow: Classes -> Checkpoints
 *
 * Backend Integration:
 * - Fetches classes from GET /api/classes endpoint
 * - Displays class information including course code, name, term, and section
 *
 * User Flow:
 * 1. User sees list of available classes
 * 2. User clicks on a class card
 * 3. User is taken to the class details page that lists associated labs
 * 4. User can pick a lab to view checkpoints
 */
export default function ClassSelector() {
  const navigate = useNavigate()
  const { user, isTeacherOrTA } = useAuth()
  const [classes, setClasses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showImportModal, setShowImportModal] = useState(false)
  const [importError, setImportError] = useState(null)
  const [importing, setImporting] = useState(false)
  const [isDraggingOver, setIsDraggingOver] = useState(false)
  const fileInputRef = useRef(null)

  const canImport = typeof isTeacherOrTA === 'function' ? isTeacherOrTA() : true

  const loadClasses = () => {
    setLoading(true)
    setError(null)

    fetch(api.classes(), { credentials: 'include' })
      .then((res) => {
        if (!res.ok) {
          throw new Error('Failed to fetch classes')
        }
        return res.json()
      })
      .then((data) => {
        setClasses(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch((err) => {
        console.error('Error fetching classes:', err)
        setError(err.message)
        setLoading(false)
      })
  }

  // Fetch classes from backend on component mount
  useEffect(() => {
    loadClasses()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!showImportModal) {
      return undefined
    }

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        closeImportModal()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [showImportModal])

  const openImportModal = () => {
    setImportError(null)
    setIsDraggingOver(false)
    setShowImportModal(true)
  }

  const closeImportModal = () => {
    if (importing) return
    setShowImportModal(false)
    setImportError(null)
    setIsDraggingOver(false)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleFileUpload = async (file) => {
    if (!file) return

    const isCsv = file.name.toLowerCase().endsWith('.csv')
    if (!isCsv) {
      setImportError('Please upload a .csv file exported from Canvas.')
      return
    }

    if (!user?.mongoId) {
      setImportError('Unable to determine your instructor account. Please try signing out and back in.')
      return
    }

    const baseName = file.name.replace(/\.[^/.]+$/, '').trim() || 'Imported Class'
    const formData = new FormData()
    formData.append('file', file)
    formData.append('instructorId', user.mongoId)
    formData.append('courseCode', baseName)
    formData.append('courseName', baseName)
    formData.append('term', `Imported ${new Date().getFullYear()}`)

    setImportError(null)
    setImporting(true)

    try {
      const response = await fetch(api.importClass(), {
        method: 'POST',
        body: formData,
        credentials: 'include'
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.error || 'Failed to import class from CSV')
      }

      const createdClass = await response.json()

      setShowImportModal(false)
      setIsDraggingOver(false)
      setImportError(null)
      setClasses((prev) => {
        const withoutNew = Array.isArray(prev) ? prev.filter((existing) => existing.id !== createdClass.id) : []
        return [createdClass, ...withoutNew]
      })
      navigate(`/classes/${createdClass.id}`)
    } catch (err) {
      console.error('Error importing class:', err)
      setImportError(err instanceof Error ? err.message : 'Failed to import class from CSV')
    } finally {
      setImporting(false)
      setIsDraggingOver(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleFileInputChange = (event) => {
    const [file] = event.target.files || []
    if (file) {
      handleFileUpload(file)
    }
  }

  const handleDrop = (event) => {
    event.preventDefault()
    event.stopPropagation()
    setIsDraggingOver(false)
    const [file] = event.dataTransfer.files || []
    if (file) {
      handleFileUpload(file)
    }
  }

  const handleDragOver = (event) => {
    event.preventDefault()
    event.stopPropagation()
    if (!isDraggingOver) {
      setIsDraggingOver(true)
    }
  }

  const handleDragLeave = (event) => {
    event.preventDefault()
    event.stopPropagation()
    setIsDraggingOver(false)
  }

  const openFilePicker = () => {
    fileInputRef.current?.click()
  }

  const handleClassOpen = (classItem) => {
    navigate(`/classes/${classItem.id}`)
  }

  if (loading) {
    return (
      <>
        <Header />
        <main className="class-selector-shell">
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '50vh',
            fontSize: '18px',
            color: '#64748b'
          }}>
            Loading classes...
          </div>
        </main>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="class-selector-shell">
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
            <div>Error loading classes: {error}</div>
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
      <main className="class-selector-shell">
        <div className="class-selector-top">
          <div className="class-selector-heading">
            <h1>Classes</h1>
            <p>View existing classes or import a Canvas gradebook to create a new one.</p>
          </div>
          {canImport && (
            <button
              type="button"
              className="class-import-trigger"
              onClick={openImportModal}
            >
              Import Gradebook CSV
            </button>
          )}
        </div>
        {/* Class Grid */}
        <section className="labs-grid-section">
          <div className="labs-grid">
            {classes.length === 0 ? (
              <div style={{
                gridColumn: '1 / -1',
                textAlign: 'center',
                padding: '40px',
                color: '#64748b'
              }}>
                No classes available
              </div>
            ) : (
              classes.map(classItem => (
                <div key={classItem.id} className="lab-card">
                  <div className="lab-card-header">
                    <h3 className="lab-card-title">
                      {classItem.courseName || classItem.courseCode || 'Untitled Class'}
                    </h3>
                    <span
                      className="lab-card-status"
                      style={{
                        backgroundColor: classItem.archived ? '#fee2e2' : '#dcfce7',
                        color: classItem.archived ? '#991b1b' : '#166534',
                        borderColor: classItem.archived ? '#fecaca' : '#bbf7d0',
                        padding: '4px 12px',
                        borderRadius: '12px',
                        fontSize: '12px',
                        fontWeight: '500'
                      }}
                    >
                      {classItem.archived ? 'Archived' : 'Active'}
                    </span>
                  </div>

                  <div className="lab-card-body">
                    <p className="lab-card-course">
                      {classItem.courseName || classItem.courseCode || 'Course name unavailable'}
                    </p>
                    <p className="lab-card-description">
                      Term: {classItem.term || 'Unknown'}
                      {classItem.section ? ` | Section ${classItem.section}` : ''}
                    </p>
                    {classItem.canvasMetadata?.courseId && (
                      <p className="lab-card-description">
                        Canvas Course ID: {classItem.canvasMetadata.courseId}
                      </p>
                    )}
                    <p className="lab-card-description">Click to view checkpoints for this class</p>
                  </div>

                  <div className="lab-card-footer">
                    <button
                      className="lab-card-button open"
                      onClick={() => handleClassOpen(classItem)}
                    >
                      View Labs
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </main>

      {showImportModal && (
        <div className="class-import-modal-overlay" onClick={closeImportModal} role="presentation">
          <div
            className="class-import-modal"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <button
              type="button"
              className="class-import-close"
              onClick={closeImportModal}
              disabled={importing}
              aria-label="Close import modal"
            >
              X
            </button>
            <h2>Import Gradebook CSV</h2>
            <p>Drag and drop your Canvas gradebook export here, or click to browse.</p>

            <div
              className={`class-import-dropzone ${isDraggingOver ? 'dragging' : ''}`}
              onDragOver={handleDragOver}
              onDrop={handleDrop}
              onDragLeave={handleDragLeave}
              onClick={openFilePicker}
              role="button"
              tabIndex={0}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  openFilePicker()
                }
              }}
            >
              <div className="class-import-icon">CSV</div>
              <p><strong>Drop your CSV file</strong> or click to select</p>
              <span className="class-import-hint">Only .csv files exported from Canvas are supported.</span>
              {importing && <span className="class-import-progress">Importing...</span>}
            </div>

            {importError && (
              <div className="class-import-error">{importError}</div>
            )}

            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              onChange={handleFileInputChange}
              style={{ display: 'none' }}
            />

            <button
              type="button"
              className="class-import-secondary"
              onClick={openFilePicker}
              disabled={importing}
            >
              {importing ? 'Importing...' : 'Browse Files'}
            </button>
          </div>
        </div>
      )}
    </>
  )
}
