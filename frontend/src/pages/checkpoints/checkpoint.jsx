import { useState, useEffect } from 'react';
import { classData } from '../../mock/classData';
import { mockCheckpoints } from '../../mock/checkpoints';
import SignOffModal from '../../components/SignOffModal';
import GroupManagementModal from '../../components/GroupManagementModal';
import './checkpoints.css';

export default function CheckpointPage() {
    const [classState, setClassState] = useState(classData);
    const [selectedSectionId, setSelectedSectionId] = useState(classData.sections[0].id);
    const [selectedGroupId, setSelectedGroupId] = useState(null);
    const [showSignOffModal, setShowSignOffModal] = useState(false);
    const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
    const [signOffNotes, setSignOffNotes] = useState('');
    const [signOffStatus, setSignOffStatus] = useState('pass'); // 'pass' or 'return'
    const [showGroupManagement, setShowGroupManagement] = useState(false);
    
    // Track checkpoint completion status per group
    const [groupCheckpoints, setGroupCheckpoints] = useState(() => {
        // Initialize with some completed checkpoints for demo
        const initial = {};
        classState.sections.forEach(section => {
            section.groups.forEach(group => {
                initial[group.id] = {};
                // Set some initial completed checkpoints based on checkpointProgress
                for (let i = 0; i < group.checkpointProgress && i < mockCheckpoints.length; i++) {
                    initial[group.id][mockCheckpoints[i].id] = {
                        completed: true,
                        completedAt: `2025-10-${10 + i}`,
                        completedBy: 'instructor',
                        notes: ''
                    };
                }
            });
        });
        return initial;
    });
    
    const selectedSection = classState.sections.find(s => s.id === selectedSectionId);
    const selectedGroup = selectedSection?.groups?.find(g => g.id === selectedGroupId);
    const currentLab = "Lab 5"; // This would come from route params in real app
    
    // Auto-select first group when section changes
    useEffect(() => {
        if (selectedSection && selectedSection.groups.length > 0) {
            setSelectedGroupId(selectedSection.groups[0].id);
        }
    }, [selectedSectionId, selectedSection]);

    const handleSignOffClick = (checkpoint, status) => {
        if (status === 'return') {
            // For undo, execute immediately without modal
            handleUndoCheckpoint(checkpoint);
        } else {
            // For sign off, show modal
            setSelectedCheckpoint(checkpoint);
            setSignOffStatus(status);
            setSignOffNotes('');
            setShowSignOffModal(true);
        }
    };

    const handleUndoCheckpoint = (checkpoint) => {
        if (!selectedGroup) return;
        
        setGroupCheckpoints(prev => {
            const updated = { ...prev };
            if (updated[selectedGroup.id]) {
                delete updated[selectedGroup.id][checkpoint.id];
            }
            return updated;
        });
        
        console.log(`Group ${selectedGroup.name} undid checkpoint ${checkpoint.name}`);
    };

    const handleSignOffConfirm = () => {
        if (!selectedCheckpoint || !selectedGroup) return;
        
        const isPassing = signOffStatus === 'pass';
        
        setGroupCheckpoints(prev => {
            const updated = { ...prev };
            if (!updated[selectedGroup.id]) {
                updated[selectedGroup.id] = {};
            }
            
            if (isPassing) {
                // Mark checkpoint as completed
                updated[selectedGroup.id][selectedCheckpoint.id] = {
                    completed: true,
                    completedAt: new Date().toISOString().split('T')[0],
                    completedBy: 'instructor',
                    notes: signOffNotes
                };
            } else {
                // Remove completion (undo)
                delete updated[selectedGroup.id][selectedCheckpoint.id];
            }
            
            return updated;
        });
        
        console.log(`Group ${selectedGroup.name} ${isPassing ? 'signed off' : 'undid'} checkpoint ${selectedCheckpoint.name}`);
        
        setShowSignOffModal(false);
        setSelectedCheckpoint(null);
        setSignOffNotes('');
    };

    // Helper function to check if a checkpoint is completed for the selected group
    const isCheckpointCompleted = (checkpointId) => {
        if (!selectedGroup) return false;
        return groupCheckpoints[selectedGroup.id]?.[checkpointId]?.completed || false;
    };

    // Helper function to get completed checkpoints count for a group
    const getCompletedCount = (groupId) => {
        if (!groupCheckpoints[groupId]) return 0;
        return Object.values(groupCheckpoints[groupId]).filter(cp => cp.completed).length;
    };

    const handleEditGroups = () => {
        setShowGroupManagement(true);
    };

    const handleUpdateGroups = (updatedSectionData) => {
        // Update the class state with the modified section data
        setClassState(prev => ({
            ...prev,
            sections: prev.sections.map(section => 
                section.id === updatedSectionData.id ? updatedSectionData : section
            )
        }));
        
        console.log('Groups updated successfully!', updatedSectionData);
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
                    <button className="action-btn secondary" onClick={handleEditGroups}>
                        <span className="btn-icon">✏️</span>
                        Manage Groups
                    </button>
                </div>
            </header>

            {/* Class and Section Selection */}
            <section className="class-section-selector">
                <div className="class-info">
                    <h2 className="class-title">{classState.name}</h2>
                </div>
                <div className="section-tabs">
                    {classState.sections.map(section => (
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
                            {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{mockCheckpoints.length}
                        </span>
                    </header>
                    
                    <div className="checkpoint-list">
                        {mockCheckpoints.map((checkpoint, index) => {
                            const isCompleted = isCheckpointCompleted(checkpoint.id);
                            const completedUpToHere = mockCheckpoints.slice(0, index + 1).filter(cp => isCheckpointCompleted(cp.id)).length;
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
                                                : index < mockCheckpoints.findIndex(cp => !isCheckpointCompleted(cp.id)) 
                                                    ? '#016836' 
                                                    : '#e5e7eb'
                                        }}></div>
                                    </div>
                                    
                                    <div className="checkpoint-details">
                                        <div className="checkpoint-main">
                                            <h3 className="checkpoint-name">{checkpoint.name}</h3>
                                            <p className="checkpoint-description">{checkpoint.description}</p>
                                            <div className="checkpoint-meta">
                                                <span className="checkpoint-points">{checkpoint.points} pt</span>
                                                {isCompleted && selectedGroup && (
                                                    <span className="checkpoint-completed-info">
                                                        Completed {groupCheckpoints[selectedGroup.id]?.[checkpoint.id]?.completedAt}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                        
                                        {selectedGroup && (
                                            <div className="checkpoint-actions">
                                                {!isCompleted ? (
                                                    <button 
                                                        className="checkpoint-btn pass"
                                                        onClick={() => handleSignOffClick(checkpoint, 'pass')}
                                                    >
                                                        Sign Off
                                                    </button>
                                                ) : (
                                                    <button 
                                                        className="checkpoint-btn return"
                                                        onClick={() => handleSignOffClick(checkpoint, 'return')}
                                                    >
                                                        Undo
                                                    </button>
                                                )}
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
                            const completedCount = getCompletedCount(group.id);
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
            <SignOffModal
                isOpen={showSignOffModal}
                onClose={() => setShowSignOffModal(false)}
                selectedCheckpoint={selectedCheckpoint}
                signOffStatus={signOffStatus}
                signOffNotes={signOffNotes}
                setSignOffNotes={setSignOffNotes}
                onConfirm={handleSignOffConfirm}
            />

            {/* Group Management Modal */}
            <GroupManagementModal
                isOpen={showGroupManagement}
                onClose={() => setShowGroupManagement(false)}
                sectionData={selectedSection}
                onUpdateGroups={handleUpdateGroups}
            />
        </main>
    );
}