const stats = [
    { id: 'students', label: 'Total Students', value: '127', helper: 'Enrolled students', icon: <span style={{ fontSize: 20 }}>👥</span> },
    { id: 'pending', label: 'Pending Check-offs', value: '23', helper: 'Awaiting review', icon: <span style={{ fontSize: 20 }}>⏰</span> },
    { id: 'active', label: 'Active Labs', value: '2', helper: '2 due this week', icon: <span style={{ fontSize: 20 }}>📄</span> },
    { id: 'pass-rate', label: 'Pass Rate', value: '89%', helper: '+3% from last lab', icon: <span style={{ fontSize: 20 }}>📈</span> },
]

const activity = [
    { id: 1, label: 'Sarah Chen - Lab 1 Checkpoint 3 PASSED', time: '2 minutes ago', tone: 'pass' },
    { id: 2, label: 'Mike Johnson - Lab 2 Checkpoint 1 RETURNED', time: '8 minutes ago', tone: 'return' },
    { id: 3, label: 'New submission: Alex Kim - Lab 1 Checkpoint 4', time: '15 minutes ago', tone: 'info' },
    { id: 4, label: 'Emma Davis - Lab 1 Complete (All checkpoints passed)', time: '32 minutes ago', tone: 'pass' },
]

const quickActions = [
    { id: 'review', label: 'Review Pending Submissions', description: 'View groups waiting for a check-off', icon: '📝' },
    { id: 'create', label: 'Create New Lab Assignment', description: 'Draft checkpoints and scoring rubric', icon: '📄' },
    { id: 'schedule', label: 'View Lab Schedule', description: 'See upcoming sessions by location', icon: '🗓️' },
    { id: 'bulk', label: 'Bulk Check-off Tool', description: 'Apply an outcome to multiple groups', icon: '💡' },
]

const schedule = [
    { id: 'session-1', time: '10:00 AM', title: 'Lab 1 Check-offs', meta: 'Room CS-201 • 8 students pending', status: 'In Progress' },
    { id: 'session-2', time: '01:30 PM', title: 'Lab 2 Check-offs', meta: 'Room CS-205 • 15 students pending', status: 'Upcoming' },
    { id: 'session-3', time: '03:00 PM', title: 'Office Hours – Lab Help', meta: 'Room CS-101 • Open session', status: 'Scheduled' },
]

const statusColors = {
    'In Progress': { bg: '#f1f5f9', color: '#0f172a' },
    'Upcoming': { bg: '#0f172a', color: '#fff' },
    'Scheduled': { bg: '#f1f5f9', color: '#0f172a' },
}

const activityDot = {
    pass: '#22c55e',
    return: '#ef4444',
    info: '#38bdf8',
}

export default function DashboardPage() {
    return (
        <main className="dash-shell">
            <header className="dash-header">
                <div>
                    <h1 className="dash-title" style={{ marginBottom: 0 }}>CS Lab Dashboard</h1>
                    <p className="dash-subtitle" style={{ marginTop: 0 }}>Track student progress and manage lab check-offs</p>
                </div>
            </header>

            <section className="dash-stats" aria-label="Summary statistics">
                {stats.map((item) => (
                    <article key={item.id} className="dash-card dash-card-stat" style={{
                        display: 'flex',
                        flexDirection: 'row',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        background: '#fff',
                        border: '2px solid',
                        borderImage: 'linear-gradient(90deg, #a78bfa, #38bdf8) 1',
                        borderRadius: 16,
                        boxShadow: '0 4px 18px rgba(2,6,23,0.06)',
                        padding: 24,
                        minWidth: 0,
                    }}>
                        <div>
                            <p className="dash-card-label" style={{ margin: 0, fontWeight: 600, color: '#64748b', fontSize: 15 }}>{item.label}</p>
                            <p className="dash-card-value" style={{ margin: '8px 0 0 0', fontSize: 28, fontWeight: 700, color: '#0f172a' }}>{item.value}</p>
                            <p className="dash-card-helper" style={{ margin: 0, color: '#64748b', fontSize: 13 }}>{item.helper}</p>
                        </div>
                        <div style={{ marginLeft: 16, color: '#cbd5e1', fontSize: 28 }}>{item.icon}</div>
                    </article>
                ))}
            </section>

            <section className="dash-main" style={{ gap: 24 }}>
                <article className="dash-panel dash-panel-wide" aria-labelledby="dash-activity" style={{
                    background: '#fff',
                    border: '1.5px solid #e5e7eb',
                    borderRadius: 16,
                    boxShadow: '0 4px 18px rgba(2,6,23,0.06)',
                    padding: 24,
                }}>
                    <header className="dash-panel-header" style={{ marginBottom: 12 }}>
                        <div>
                            <h2 id="dash-activity" className="dash-panel-title" style={{ fontSize: 20, margin: 0 }}>
                                Recent Check-off Activity
                            </h2>
                            <p className="dash-panel-subtitle" style={{ margin: 0, color: '#64748b', fontSize: 14 }}>Latest student submissions and check-offs.</p>
                        </div>
                    </header>
                    <ul className="dash-activity" style={{ margin: 0, padding: 0, listStyle: 'none', display: 'grid', gap: 14 }}>
                        {activity.map((item) => (
                            <li key={item.id} className="dash-activity-row" style={{ display: 'grid', gridTemplateColumns: '18px 1fr', gap: 12, alignItems: 'flex-start' }}>
                                <span aria-hidden="true" className="dash-activity-indicator" style={{
                                    width: 12,
                                    height: 12,
                                    borderRadius: 999,
                                    marginTop: 4,
                                    background: activityDot[item.tone],
                                    display: 'inline-block',
                                }} />
                                <div>
                                    <p className="dash-activity-label" style={{ margin: 0, fontWeight: 600 }}>{item.label}</p>
                                    <p className="dash-activity-time" style={{ margin: '4px 0 0 0', color: '#64748b', fontSize: 13 }}>{item.time}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                </article>

                <article className="dash-panel" aria-labelledby="dash-actions" style={{
                    background: '#fff',
                    border: '1.5px solid #e5e7eb',
                    borderRadius: 16,
                    boxShadow: '0 4px 18px rgba(2,6,23,0.06)',
                    padding: 24,
                }}>
                    <header className="dash-panel-header" style={{ marginBottom: 12 }}>
                        <div>
                            <h2 id="dash-actions" className="dash-panel-title" style={{ fontSize: 20, margin: 0 }}>
                                Quick Actions
                            </h2>
                            <p className="dash-panel-subtitle" style={{ margin: 0, color: '#64748b', fontSize: 14 }}>Common check-off tasks</p>
                        </div>
                    </header>
                    <ul className="dash-actions" style={{ margin: 0, padding: 0, listStyle: 'none', display: 'grid', gap: 12 }}>
                        {quickActions.map((item) => (
                            <li key={item.id}>
                                <button type="button" className="dash-action" style={{
                                    width: '100%',
                                    border: 'none',
                                    background: '#fff',
                                    borderLeft: '6px solid',
                                    borderImage: 'linear-gradient(180deg, #a78bfa, #38bdf8) 1',
                                    color: '#0f172a',
                                    borderRadius: 14,
                                    padding: '14px 16px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 12,
                                    textAlign: 'left',
                                    cursor: 'pointer',
                                    fontSize: 16,
                                    fontWeight: 600,
                                    boxShadow: '0 2px 8px 0 rgba(99,102,241,0.06)',
                                }} onClick={() => alert(item.label)}>
                                    <span className="dash-action-icon" aria-hidden="true" style={{ fontSize: 20 }}>
                                        {item.icon}
                                    </span>
                                    <span>
                                        <span className="dash-action-label" style={{ display: 'block', fontWeight: 600 }}>{item.label}</span>
                                        <span className="dash-action-helper" style={{ display: 'block', marginTop: 4, fontSize: 13, color: '#64748b' }}>{item.description}</span>
                                    </span>
                                </button>
                            </li>
                        ))}
                    </ul>
                </article>
            </section>

            <section className="dash-panel" aria-labelledby="dash-schedule" style={{
                background: '#fff',
                border: '1.5px solid #e5e7eb',
                borderRadius: 16,
                boxShadow: '0 4px 18px rgba(2,6,23,0.06)',
                padding: 24,
                marginTop: 24,
            }}>
                <header className="dash-panel-header" style={{ marginBottom: 12 }}>
                    <div>
                        <h2 id="dash-schedule" className="dash-panel-title" style={{ fontSize: 20, margin: 0 }}>
                            Today&apos;s Lab Sessions
                        </h2>
                        <p className="dash-panel-subtitle" style={{ margin: 0, color: '#64748b', fontSize: 14 }}>Scheduled check-off sessions and office hours.</p>
                    </div>
                </header>
                <ul className="dash-schedule" style={{ margin: 0, padding: 0, listStyle: 'none', display: 'grid', gap: 16 }}>
                    {schedule.map((item) => (
                        <li key={item.id} className="dash-schedule-row" style={{
                            display: 'grid',
                            gridTemplateColumns: '80px 1fr auto',
                            gap: 18,
                            alignItems: 'center',
                            padding: '12px 0',
                            borderBottom: '1px solid #e2e8f0',
                        }}>
                            <div className="dash-schedule-time" style={{ display: 'grid', justifyItems: 'center', gap: 2 }}>
                                <span className="dash-schedule-hour" style={{ fontSize: 18, fontWeight: 700 }}>
                                    {item.time.split(' ')[0]}
                                </span>
                                <span className="dash-schedule-meridiem" style={{ fontSize: 12, color: '#64748b' }}>
                                    {item.time.split(' ')[1]}
                                </span>
                            </div>
                            <div className="dash-schedule-details">
                                <p className="dash-schedule-title" style={{ margin: '0 0 4px 0', fontWeight: 600 }}>{item.title}</p>
                                <p className="dash-schedule-meta" style={{ margin: 0, color: '#64748b', fontSize: 13 }}>{item.meta}</p>
                            </div>
                            <span className="dash-schedule-status" style={{
                                padding: '6px 12px',
                                borderRadius: 999,
                                background: item.status === 'Upcoming'
                                    ? 'linear-gradient(90deg, #a78bfa, #38bdf8)'
                                    : statusColors[item.status].bg,
                                color: item.status === 'Upcoming'
                                    ? '#fff'
                                    : statusColors[item.status].color,
                                border: '1px solid #cbd5e1',
                                fontSize: 13,
                                fontWeight: 600,
                                textTransform: 'capitalize',
                                justifySelf: 'flex-end',
                            }}>{item.status}</span>
                        </li>
                    ))}
                </ul>
            </section>
        </main>
    )
}