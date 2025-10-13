import { useState, useEffect } from 'react';
import { groups } from '../../mock/groups';
import '../dashboard/dashboard.css';

// Mock class/section data
const classData = {
    id: 'cs-101',
    name: 'CS 101: Introduction to Programming',
    sections: [
        {
            id: 'section-1',
            name: 'Section 1',
            time: 'MWF 9:00-9:50 AM',
            groups: [
                {
                    id: 'grp-s1-1',
                    name: 'Alex & Sarah',
                    members: [
                        { id: 'u1', name: 'Alex Rivera', email: 'alex.rivera@email.com' },
                        { id: 'u2', name: 'Sarah Chen', email: 'sarah.chen@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 3
                },
                {
                    id: 'grp-s1-2',
                    name: 'Jordan & Taylor',
                    members: [
                        { id: 'u3', name: 'Jordan Kim', email: 'jordan.kim@email.com' },
                        { id: 'u4', name: 'Taylor Brooks', email: 'taylor.brooks@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 2
                },
                {
                    id: 'grp-s1-3',
                    name: 'Morgan & Casey',
                    members: [
                        { id: 'u5', name: 'Morgan Davis', email: 'morgan.davis@email.com' },
                        { id: 'u6', name: 'Casey Johnson', email: 'casey.johnson@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 4
                },
                {
                    id: 'grp-s1-4',
                    name: 'Riley & Quinn & Avery',
                    members: [
                        { id: 'u7', name: 'Riley Martinez', email: 'riley.martinez@email.com' },
                        { id: 'u8', name: 'Quinn Anderson', email: 'quinn.anderson@email.com' },
                        { id: 'u9', name: 'Avery Wilson', email: 'avery.wilson@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 1
                }
            ]
        },
        {
            id: 'section-2',
            name: 'Section 2',
            time: 'MWF 11:00-11:50 AM',
            groups: [
                {
                    id: 'grp-s2-1',
                    name: 'Blake & Drew',
                    members: [
                        { id: 'u10', name: 'Blake Thompson', email: 'blake.thompson@email.com' },
                        { id: 'u11', name: 'Drew Garcia', email: 'drew.garcia@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 2
                },
                {
                    id: 'grp-s2-2',
                    name: 'Jamie & Sage & River',
                    members: [
                        { id: 'u12', name: 'Jamie Lopez', email: 'jamie.lopez@email.com' },
                        { id: 'u13', name: 'Sage Miller', email: 'sage.miller@email.com' },
                        { id: 'u14', name: 'River Clark', email: 'river.clark@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 3
                },
                {
                    id: 'grp-s2-3',
                    name: 'Phoenix & Rowan',
                    members: [
                        { id: 'u15', name: 'Phoenix Lee', email: 'phoenix.lee@email.com' },
                        { id: 'u16', name: 'Rowan White', email: 'rowan.white@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 5
                },
                {
                    id: 'grp-s2-4',
                    name: 'Emery & Dakota',
                    members: [
                        { id: 'u17', name: 'Emery Harris', email: 'emery.harris@email.com' },
                        { id: 'u18', name: 'Dakota Moore', email: 'dakota.moore@email.com' }
                    ],
                    status: 'Active',
                    checkpointProgress: 1
                }
            ]
        }
    ]
};

// Mock checkpoint data with more comprehensive structure - Lab 5 specific
const mockCheckpoints = [
    { id: 'cp-1', name: 'Environment Setup', description: 'Configure development environment and install required dependencies', completed: true, completedAt: '2025-10-10', completedBy: 'u2', points: 10 },
    { id: 'cp-2', name: 'Database Schema', description: 'Design and implement database schema with proper relationships', completed: true, completedAt: '2025-10-11', completedBy: 'u2', points: 20 },
    { id: 'cp-3', name: 'API Endpoints', description: 'Create RESTful API endpoints with proper error handling', completed: true, completedAt: '2025-10-12', completedBy: 'u1', points: 25 },
    { id: 'cp-4', name: 'Frontend Components', description: 'Develop responsive React components with proper state management', completed: false, points: 25 },
    { id: 'cp-5', name: 'Testing & Validation', description: 'Write and execute comprehensive test cases', completed: false, points: 15 },
    { id: 'cp-6', name: 'Deployment & Documentation', description: 'Deploy application and create complete documentation', completed: false, points: 15 }
];

export default function CheckpointPage() {
    const [selectedSectionId, setSelectedSectionId] = useState(classData.sections[0].id);
    const [selectedGroupId, setSelectedGroupId] = useState(null);
    const [showSignOffModal, setShowSignOffModal] = useState(false);
    const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
    const [signOffNotes, setSignOffNotes] = useState('');
    const [signOffStatus, setSignOffStatus] = useState('pass'); // 'pass' or 'return'
    
    const selectedSection = classData.sections.find(s => s.id === selectedSectionId);
    const selectedGroup = selectedSection?.groups?.find(g => g.id === selectedGroupId);
    const currentLab = "Lab 5"; // This would come from route params in real app
    
    // Auto-select first group when section changes
    useEffect(() => {
        if (selectedSection && selectedSection.groups.length > 0) {
            setSelectedGroupId(selectedSection.groups[0].id);
        }
    }, [selectedSectionId, selectedSection]);

    const handleSignOffClick = (checkpoint, status) => {
        setSelectedCheckpoint(checkpoint);
        setSignOffStatus(status);
        setSignOffNotes('');
        setShowSignOffModal(true);
    };

    const handleSignOffConfirm = () => {
        if (!selectedCheckpoint) return;
        
        const isPassing = signOffStatus === 'pass';
        // Update checkpoint progress for the selected group
        if (isPassing && selectedGroup) {
            const currentProgress = selectedGroup.checkpointProgress;
            const checkpointIndex = mockCheckpoints.findIndex(cp => cp.id === selectedCheckpoint.id);
            
            // Only increment if this checkpoint hasn't been passed yet
            if (checkpointIndex >= currentProgress) {
                // Update the group's progress in the class data
                // In a real app, this would be sent to the backend
                console.log(`Group ${selectedGroup.name} passed checkpoint ${selectedCheckpoint.name}`);
                
                // For demo purposes, we could update local state here
                // but since we're using mock data structure, we'll just log it
            }
        }
        
        setShowSignOffModal(false);
        setSelectedCheckpoint(null);
        setSignOffNotes('');
    };

    const handleAddGroup = () => {
        alert('Add Group functionality - opens group creation modal');
    };

    const handleEditGroups = () => {
        alert('Edit Groups functionality - opens group management interface');
    };
    
    return (
        <main className="checkpoint-shell">
            {/* Header with breadcrumb and actions */}
            <header className="checkpoint-header">
                <div className="checkpoint-nav">
                    <button className="breadcrumb-back" onClick={() => window.history.back()}>
                        <span className="back-arrow">←</span>
                        <span>Labs</span>
                    </button>
                    <span className="breadcrumb-separator">/</span>
                    <h1 className="checkpoint-title">{currentLab}</h1>
                </div>
                <div className="checkpoint-actions">
                    <button className="action-btn secondary" onClick={handleAddGroup}>
                        <span className="btn-icon">+</span>
                        Add Group
                    </button>
                    <button className="action-btn secondary" onClick={handleEditGroups}>
                        <span className="btn-icon">✏️</span>
                        Edit Groups
                    </button>
                </div>
            </header>

            {/* Class and Section Selection */}
            <section className="class-section-selector">
                <div className="class-info">
                    <h2 className="class-title">{classData.name}</h2>
                </div>
                <div className="section-tabs">
                    {classData.sections.map(section => (
                        <button
                            key={section.id}
                            className={`section-tab ${selectedSectionId === section.id ? 'active' : ''}`}
                            onClick={() => setSelectedSectionId(section.id)}
                        >
                            <span className="section-name">{section.name}</span>
                            <span className="section-time">{section.time}</span>
                            <span className="section-count">{section.groups.length} groups</span>
                        </button>
                    ))}
                </div>
            </section>

            <div className="checkpoint-content">
                {/* Checkpoint List Panel */}
                <section className="checkpoint-panel" aria-labelledby="checkpoints-title">
                    <header className="panel-header">
                        <h2 id="checkpoints-title" className="panel-title">
                            Checkpoints
                        </h2>
                        <span className="checkpoint-count">
                            {mockCheckpoints.filter(cp => cp.completed).length}/{mockCheckpoints.length}
                        </span>
                    </header>
                    
                    <div className="checkpoint-list">
                        {mockCheckpoints.map((checkpoint, index) => {
                            const isCompleted = checkpoint.completed;
                            const completedUpToHere = mockCheckpoints.slice(0, index + 1).filter(cp => cp.completed).length;
                            const progress = (completedUpToHere / mockCheckpoints.length) * 100;
                            
                            return (
                                <div key={checkpoint.id} className={`checkpoint-item ${isCompleted ? 'completed' : 'pending'}`}>
                                    <div className="checkpoint-indicator">
                                        <div className="checkpoint-number">
                                            {isCompleted ? '✓' : index + 1}
                                        </div>
                                        <div className="checkpoint-progress-line" style={{ 
                                            background: isCompleted 
                                                ? '#016836' 
                                                : index < mockCheckpoints.findIndex(cp => !cp.completed) 
                                                    ? '#016836' 
                                                    : '#e5e7eb'
                                        }}></div>
                                    </div>
                                    
                                    <div className="checkpoint-details">
                                        <div className="checkpoint-main">
                                            <h3 className="checkpoint-name">{checkpoint.name}</h3>
                                            <p className="checkpoint-description">{checkpoint.description}</p>
                                            <div className="checkpoint-meta">
                                                <span className="checkpoint-points">{checkpoint.points} pts</span>
                                                {isCompleted && (
                                                    <span className="checkpoint-completed-info">
                                                        Completed {checkpoint.completedAt}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                        
                                        {!isCompleted && selectedGroup && (
                                            <div className="checkpoint-actions">
                                                <button 
                                                    className="checkpoint-btn pass"
                                                    onClick={() => handleSignOffClick(checkpoint, 'pass')}
                                                >
                                                    Pass
                                                </button>
                                                <button 
                                                    className="checkpoint-btn return"
                                                    onClick={() => handleSignOffClick(checkpoint, 'return')}
                                                >
                                                    Return
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>

                {/* Groups Panel */}
                <section className="groups-panel" aria-labelledby="groups-title">
                    <header className="panel-header">
                        <h2 id="groups-title" className="panel-title">
                            {selectedSection?.name} Groups
                        </h2>
                        <span className="groups-count">{selectedSection?.groups.length || 0} groups</span>
                    </header>
                    
                    <div className="groups-list">
                        {selectedSection?.groups.map(group => {
                            const completedCount = group.checkpointProgress;
                            const totalCount = mockCheckpoints.length;
                            const progressPercent = Math.round((completedCount / totalCount) * 100);
                            const isSelected = group.id === selectedGroupId;
                            
                            return (
                                <div 
                                    key={group.id} 
                                    className={`group-card ${isSelected ? 'selected' : ''}`}
                                    onClick={() => setSelectedGroupId(group.id)}
                                >
                                    <div className="group-card-header">
                                        <h3 className="group-name">{group.name}</h3>
                                        <span className={`group-status ${group.status.toLowerCase()}`}>
                                            {group.status}
                                        </span>
                                    </div>
                                    
                                    <div className="group-card-body">
                                        <div className="group-members">
                                            <div className="members-list">
                                                {group.members.map((member, index) => (
                                                    <span key={member.id} className="member-name">
                                                        {member.name}{index < group.members.length - 1 && ', '}
                                                    </span>
                                                ))}
                                            </div>
                                            <span className="members-count">
                                                {group.members.length} member{group.members.length !== 1 ? 's' : ''}
                                            </span>
                                        </div>
                                        
                                        <div className="group-progress">
                                            <div className="progress-header">
                                                <span className="progress-text">Checkpoints</span>
                                                <span className="progress-value">{completedCount}/{totalCount}</span>
                                            </div>
                                            <div className="progress-bar">
                                                <div 
                                                    className="progress-fill" 
                                                    style={{ width: `${progressPercent}%` }}
                                                ></div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            </div>

            {/* Sign-off Modal */}
            {showSignOffModal && (
                <div className="modal-overlay" onClick={() => setShowSignOffModal(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <header className="modal-header">
                            <h3 className="modal-title">
                                {signOffStatus === 'pass' ? 'Pass' : 'Return'} Checkpoint
                            </h3>
                            <button 
                                className="modal-close"
                                onClick={() => setShowSignOffModal(false)}
                            >
                                ×
                            </button>
                        </header>
                        
                        <div className="modal-body">
                            <div className="checkpoint-summary">
                                <h4>{selectedCheckpoint?.name}</h4>
                                <p>{selectedCheckpoint?.description}</p>
                                <span className="checkpoint-points-badge">
                                    {selectedCheckpoint?.points} points
                                </span>
                            </div>
                            
                            <div className="notes-section">
                                <label htmlFor="signoff-notes" className="notes-label">
                                    Notes {signOffStatus === 'return' && <span className="required">*</span>}
                                </label>
                                <textarea
                                    id="signoff-notes"
                                    className="notes-textarea"
                                    placeholder={signOffStatus === 'pass' 
                                        ? "Optional feedback or comments..." 
                                        : "Please explain what needs to be improved..."
                                    }
                                    value={signOffNotes}
                                    onChange={(e) => setSignOffNotes(e.target.value)}
                                    rows={4}
                                />
                            </div>
                        </div>
                        
                        <footer className="modal-footer">
                            <button 
                                className="modal-btn secondary" 
                                onClick={() => setShowSignOffModal(false)}
                            >
                                Cancel
                            </button>
                            <button 
                                className={`modal-btn primary ${signOffStatus}`}
                                onClick={handleSignOffConfirm}
                                disabled={signOffStatus === 'return' && !signOffNotes.trim()}
                            >
                                {signOffStatus === 'pass' ? 'Confirm Pass' : 'Confirm Return'}
                            </button>
                        </footer>
                    </div>
                </div>
            )}
        </main>
    );
}