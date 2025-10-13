import { useMemo, useState } from 'react'
import './lab-selector.css';

const labs = [
  {
    id: 'lab-01',
    name: 'Lab 1: Variables & Data Types',
    summary: 'Basic variable declaration, primitive types, and type conversion',
    due: 'Sept 25, 2024',
    status: 'active',
    checkpoints: 4,
    totalCheckpoints: 5,
    pending: 8,
  },
  {
    id: 'lab-02',
    name: 'Lab 2: Control Structures',
    summary: 'If statements, loops, and conditional logic implementation',
    due: 'Oct 2, 2024',
    status: 'active',
    checkpoints: 2,
    totalCheckpoints: 7,
    pending: 15,
  },
  {
    id: 'lab-03',
    name: 'Lab 3: Functions & Methods',
    summary: 'Function definition, parameters, return values, and scope',
    due: 'Oct 9, 2024',
    status: 'upcoming',
    checkpoints: 0,
    totalCheckpoints: 6,
    pending: 0,
  },
  {
    id: 'lab-04',
    name: 'Lab 4: Arrays & Collections',
    summary: 'Array manipulation, iteration, and basic data structures',
    due: 'Oct 16, 2024',
    status: 'upcoming',
    checkpoints: 0,
    totalCheckpoints: 8,
    pending: 0,
  },
]

const statusLabels = {
  active: 'Active',
  upcoming: 'Upcoming',
  archived: 'Archived',
}

export default function LabSelector() {
  const [query, setQuery] = useState('')

  const filteredLabs = useMemo(() => {
    if (!query.trim()) return labs
    const normalized = query.trim().toLowerCase()
    return labs.filter(lab =>
      lab.name.toLowerCase().includes(normalized) ||
      lab.summary.toLowerCase().includes(normalized)
    )
  }, [query])

  return (
    <main className="lab-shell" style={{ background: '#f6f8fc', minHeight: '100vh' }}>
      <header className="lab-header" style={{ marginBottom: 8 }}>
        <h1 className="lab-title" style={{ fontSize: 28, marginBottom: 4 }}>Lab Check-Off Center</h1>
        <p className="lab-subtitle" style={{ color: '#64748b', marginBottom: 0 }}>
          Select CS lab assignments to review and check off student work
        </p>
        <div className="lab-controls" style={{ marginTop: 18 }}>
          <label className="lab-search-label" htmlFor="lab-query">Search labs</label>
          <input
            id="lab-query"
            type="search"
            className="lab-search"
            placeholder="Filter by name"
            value={query}
            onChange={e => setQuery(e.target.value)}
            style={{ marginRight: 12 }}
          />
        </div>
      </header>
      <section className="gc-grid-wrap" style={{ padding: 0 }}>
        <div className="gc-grid" style={{ gap: 24 }}>
          {filteredLabs.map(lab => {
            const isActive = lab.status === 'active'
            const isUpcoming = lab.status === 'upcoming'
            const progress = lab.totalCheckpoints
              ? Math.round((lab.checkpoints / lab.totalCheckpoints) * 100)
              : 0
            return (
              <div
                className="gc-card"
                key={lab.id}
                style={{
                  minWidth: 320,
                  maxWidth: 360,
                  position: 'relative',
                  border: '1.5px solid',
                  borderImage: 'linear-gradient(90deg, #6366f1, #22d3ee) 1',
                  boxShadow: '0 8px 32px 0 rgba(99,102,241,0.08), 0 1.5px 0 0 #22d3ee',
                  background: 'linear-gradient(135deg, #f8fafc 80%, #e0f2fe 100%)',
                }}
              >
                <div className="gc-header" style={{ marginBottom: 4 }}>
                  <h2 className="gc-title" style={{ fontSize: 18, fontWeight: 700, margin: 0 }}>
                    {lab.name}
                  </h2>
                  <span
                    className="gc-status"
                    style={{
                      fontSize: 13,
                      fontWeight: 600,
                      background: isActive ? '#eef2ff' : '#f1f5f9',
                      color: isActive ? '#3730a3' : '#64748b',
                      borderColor: isActive ? '#c7d2fe' : '#d1d5db',
                      marginLeft: 8,
                    }}
                  >
                    {statusLabels[lab.status]}
                  </span>
                </div>
                <div className="gc-meta" style={{ marginBottom: 8 }}>
                  <div style={{ color: '#64748b', fontSize: 15 }}>{lab.summary}</div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                  <div>
                    <div style={{ color: '#64748b', fontSize: 14 }}>Due Date:</div>
                    <div style={{ fontWeight: 600 }}>{lab.due}</div>
                  </div>
                  <div>
                    <div style={{ color: '#64748b', fontSize: 14 }}>Checkpoints:</div>
                    <div style={{ fontWeight: 600 }}>{lab.checkpoints}/{lab.totalCheckpoints}</div>
                  </div>
                  <div>
                    <div style={{ color: '#64748b', fontSize: 14 }}>Pending Students:</div>
                    <div style={{
                      fontWeight: 600,
                      color: lab.pending > 0 ? '#dc2626' : '#64748b',
                      fontSize: 16,
                    }}>{lab.pending}</div>
                  </div>
                </div>
                <div style={{ height: 8, background: '#f1f5f9', borderRadius: 6, margin: '8px 0 12px 0', overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${progress}%`,
                      height: '100%',
                      background: 'linear-gradient(90deg, #22d3ee, #22c55e)',
                      borderRadius: 6,
                      transition: 'width 0.3s',
                    }}
                  />
                </div>
                <div className="gc-actions">
                  <button
                    className="button"
                    style={{
                      background: isActive
                        ? 'linear-gradient(90deg, #6366f1, #22d3ee)'
                        : 'linear-gradient(90deg, #cbd5e1, #e0e7ef)',
                      color: isActive ? '#fff' : '#6b7280',
                      fontWeight: 700,
                      borderRadius: 12,
                      border: 0,
                      width: '100%',
                      height: 44,
                      cursor: isActive ? 'pointer' : 'not-allowed',
                      marginBottom: 0,
                      fontSize: 16,
                      boxShadow: isActive ? '0 2px 8px 0 rgba(99,102,241,0.10)' : undefined,
                      opacity: isActive ? 1 : 0.7,
                      transition: 'background 0.2s, color 0.2s',
                    }}
                    disabled={!isActive}
                    onClick={() => isActive && alert(`Reviewing submissions for ${lab.name}`)}
                  >
                    {isActive ? 'Review Submissions' : 'Not Available'}
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