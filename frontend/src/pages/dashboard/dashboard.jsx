import React from 'react';
import './dashboard.css';

const stats = [
    { 
        id: 'students', 
        label: 'Total Students', 
        value: '127', 
        helper: 'Enrolled students', 
        icon: '👥',
        trend: '+12 this semester'
    },
    { 
        id: 'pending', 
        label: 'Pending Check-offs', 
        value: '23', 
        helper: 'Awaiting review', 
        icon: '⏰',
        trend: '8 due today'
    },
    { 
        id: 'active', 
        label: 'Active Labs', 
        value: '2', 
        helper: '2 due this week', 
        icon: '📄',
        trend: 'Lab 3 starts Monday'
    },
    { 
        id: 'pass-rate', 
        label: 'Pass Rate', 
        value: '89%', 
        helper: '+3% from last lab', 
        icon: '📈',
        trend: 'Above average'
    },
]

const activity = [
    { 
        id: 1, 
        student: 'Sarah Chen',
        action: 'Lab 1 Checkpoint 3',
        result: 'PASSED',
        time: '2 minutes ago', 
        tone: 'pass' 
    },
    { 
        id: 2, 
        student: 'Mike Johnson',
        action: 'Lab 2 Checkpoint 1',
        result: 'RETURNED',
        time: '8 minutes ago', 
        tone: 'return' 
    },
    { 
        id: 3, 
        student: 'Alex Kim',
        action: 'Lab 1 Checkpoint 4',
        result: 'SUBMITTED',
        time: '15 minutes ago', 
        tone: 'info' 
    },
    { 
        id: 4, 
        student: 'Emma Davis',
        action: 'Lab 1 Complete',
        result: 'ALL PASSED',
        time: '32 minutes ago', 
        tone: 'pass' 
    },
    { 
        id: 5, 
        student: 'James Wilson',
        action: 'Lab 2 Checkpoint 2',
        result: 'SUBMITTED',
        time: '1 hour ago', 
        tone: 'info' 
    },
]

const quickActions = [
    { 
        id: 'review', 
        label: 'Review Pending Submissions', 
        description: 'View groups waiting for check-off', 
        icon: '📝',
        count: '23 pending'
    },
    { 
        id: 'create', 
        label: 'Create New Lab Assignment', 
        description: 'Draft checkpoints and scoring rubric', 
        icon: '📄',
        count: null
    },
    { 
        id: 'schedule', 
        label: 'View Lab Schedule', 
        description: 'See upcoming sessions by location', 
        icon: '🗓️',
        count: '3 sessions today'
    },
    { 
        id: 'bulk', 
        label: 'Bulk Check-off Tool', 
        description: 'Apply outcome to multiple groups', 
        icon: '⚡',
        count: null
    },
]

const schedule = [
    { 
        id: 'session-1', 
        time: '10:00 AM', 
        title: 'Lab 1 Check-offs', 
        location: 'Room CS-201',
        students: 8,
        status: 'in-progress' 
    },
    { 
        id: 'session-2', 
        time: '01:30 PM', 
        title: 'Lab 2 Check-offs', 
        location: 'Room CS-205',
        students: 15,
        status: 'upcoming' 
    },
    { 
        id: 'session-3', 
        time: '03:00 PM', 
        title: 'Office Hours – Lab Help', 
        location: 'Room CS-101',
        students: null,
        status: 'scheduled' 
    },
]

export default function DashboardPage() {
    return (
        <main className="dash-shell">
            <header className="dash-header">
                <h1 className="dash-title">CS Lab Dashboard</h1>
                <p className="dash-subtitle">Track student progress and manage lab check-offs</p>
            </header>

            <section className="dash-stats" aria-label="Summary statistics">
                {stats.map((stat) => (
                    <article key={stat.id} className="dash-card-stat">
                        <div className="dash-card-content">
                            <div className="dash-card-info">
                                <p className="dash-card-label">{stat.label}</p>
                                <p className="dash-card-value">{stat.value}</p>
                                <p className="dash-card-helper">{stat.helper}</p>
                            </div>
                            <div className="dash-card-icon">{stat.icon}</div>
                        </div>
                    </article>
                ))}
            </section>

            <section className="dash-main">
                <article className="dash-panel" aria-labelledby="dash-activity">
                    <header className="dash-panel-header">
                        <h2 id="dash-activity" className="dash-panel-title">
                            Recent Check-off Activity
                        </h2>
                        <p className="dash-panel-subtitle">Latest student submissions and check-offs</p>
                    </header>
                    <ul className="dash-activity">
                        {activity.map((item) => (
                            <li key={item.id} className="dash-activity-row">
                                <span 
                                    className={`dash-activity-indicator ${item.tone}`}
                                    aria-hidden="true"
                                />
                                <div className="dash-activity-content">
                                    <p className="dash-activity-label">
                                        <strong>{item.student}</strong> - {item.action} 
                                        <span className={`activity-result ${item.tone}`}> {item.result}</span>
                                    </p>
                                    <p className="dash-activity-time">{item.time}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                </article>

                <article className="dash-panel" aria-labelledby="dash-actions">
                    <header className="dash-panel-header">
                        <h2 id="dash-actions" className="dash-panel-title">
                            Quick Actions
                        </h2>
                        <p className="dash-panel-subtitle">Common check-off tasks</p>
                    </header>
                    <ul className="dash-actions">
                        {quickActions.map((action) => (
                            <li key={action.id}>
                                <button 
                                    type="button" 
                                    className="dash-action"
                                    onClick={() => alert(`Navigate to: ${action.label}`)}
                                >
                                    <span className="dash-action-icon" aria-hidden="true">
                                        {action.icon}
                                    </span>
                                    <div className="dash-action-content">
                                        <p className="dash-action-label">{action.label}</p>
                                        <p className="dash-action-description">
                                            {action.description}
                                            {action.count && <span className="action-count"> • {action.count}</span>}
                                        </p>
                                    </div>
                                </button>
                            </li>
                        ))}
                    </ul>
                </article>
            </section>

            <section className="dash-panel" aria-labelledby="dash-schedule">
                <header className="dash-panel-header">
                    <h2 id="dash-schedule" className="dash-panel-title">
                        Today's Lab Sessions
                    </h2>
                    <p className="dash-panel-subtitle">Scheduled check-off sessions and office hours</p>
                </header>
                <ul className="dash-schedule">
                    {schedule.map((session) => (
                        <li key={session.id} className="dash-schedule-row">
                            <div className="dash-schedule-time">
                                <span className="dash-schedule-hour">
                                    {session.time.split(' ')[0]}
                                </span>
                                <span className="dash-schedule-meridiem">
                                    {session.time.split(' ')[1]}
                                </span>
                            </div>
                            <div className="dash-schedule-details">
                                <p className="dash-schedule-title">{session.title}</p>
                                <p className="dash-schedule-meta">
                                    {session.location}
                                    {session.students && ` • ${session.students} students`}
                                    {session.students && session.status === 'upcoming' && ' pending'}
                                    {!session.students && ' • Open session'}
                                </p>
                            </div>
                            <span className={`dash-schedule-status ${session.status}`}>
                                {session.status.replace('-', ' ')}
                            </span>
                        </li>
                    ))}
                </ul>
            </section>
        </main>
    )
}