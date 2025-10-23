import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mockCheckpoints } from '../../mock/checkpoints';
import SignOffModal from '../../components/SignOffModal';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import './checkpoints.css';

/**
 * CheckpointPage Component
 *
 * Displays checkpoints for a specific lab with group selection.
 * This is the second step in the simplified user flow: Labs -> Checkpoints
 *
 * URL Parameters:
 * - labId: The ID of the lab (required)
 * - groupId: The ID of the group (optional - for backward compatibility and direct group access)
 *
 * Features:
 * - Auto-selects first group if no groupId is provided
 * - Displays all groups for the lab with progress indicators
 * - Allows switching between groups without navigation
 * - Supports both new simplified flow and legacy group-specific URLs
 */
export default function CheckpointPage() {
    const { labId, groupId } = useParams();
    const navigate = useNavigate();
    const [groups, setGroups] = useState([]);
    const [selectedGroupId, setSelectedGroupId] = useState(groupId || null);
    const [showSignOffModal, setShowSignOffModal] = useState(false);
    const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
    const [signOffNotes, setSignOffNotes] = useState('');
    const [signOffStatus, setSignOffStatus] = useState('pass'); // 'pass' or 'return'
    const [showGroupManagement, setShowGroupManagement] = useState(false);
    const [group, setGroup] = useState(null);
    const [lab, setLab] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Track checkpoint completion status per group
    const [groupCheckpoints, setGroupCheckpoints] = useState({});

    const selectedGroup = groups.find(g => g.id === selectedGroupId);
    const currentLab = lab?.courseId || "Lab"; // Use actual lab data

    // Fetch lab and group data when component mounts
    useEffect(() => {
        if (labId) {
            setLoading(true);
            // Fetch both lab info and groups data
            Promise.all([
                fetch(`http://localhost:8080/lti/labs`).then(res => res.json()),
                fetch(`http://localhost:8080/lti/labs/${labId}/groups`).then(res => {
                    if (!res.ok) throw new Error('Failed to fetch groups');
                    return res.json();
                })
            ])
                .then(([allLabs, groupsData]) => {
                    const currentLab = allLabs.find(l => l.id === labId);
                    setLab(currentLab);

                    const groupsArray = Array.isArray(groupsData) ? groupsData : [];
                    setGroups(groupsArray);

                    // Set selected group if groupId is provided
                    if (groupId) {
                        const foundGroup = groupsArray.find(g => g.id === groupId);
                        setGroup(foundGroup);
                        setSelectedGroupId(groupId);
                    } else if (groupsArray.length > 0) {
                        // Auto-select first group if no specific group is selected
                        setSelectedGroupId(groupsArray[0].id);
                    }

                    // Initialize checkpoint progress for each group
                    const initial = {};
                    groupsArray.forEach(group => {
                        initial[group.id] = {};
                        // You can add logic here to fetch actual checkpoint progress from backend
                        // For now, keeping it empty until we have checkpoint progress API
                    });
                    setGroupCheckpoints(initial);

                    setLoading(false);
                })
                .catch(err => {
                    console.error('Error fetching data:', err);
                    setError(err.message);
                    setLoading(false);
                });
        }
    }, [labId, groupId]);

    // Update selected group when selectedGroupId changes
    useEffect(() => {
        if (selectedGroupId && groups.length > 0) {
            const foundGroup = groups.find(g => g.id === selectedGroupId);
            setGroup(foundGroup);
        }
    }, [selectedGroupId, groups]);

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

    const handleSignOffConfirm = async () => {
        if (!selectedCheckpoint || !selectedGroup) return;

        const isPassing = signOffStatus === 'pass';

        // Build next state to check completion before setting it
        const next = { ...groupCheckpoints };
        if (!next[selectedGroup.id]) next[selectedGroup.id] = {};

        if (isPassing) {
            next[selectedGroup.id][selectedCheckpoint.id] = {
                completed: true,
                completedAt: new Date().toISOString().split('T')[0],
                completedBy: 'instructor',
                notes: signOffNotes
            };
        } else {
            delete next[selectedGroup.id][selectedCheckpoint.id];
        }

        // Determine if all checkpoints are completed for this group
        const allCompleted = mockCheckpoints.every(cp => next[selectedGroup.id]?.[cp.id]?.completed);

        // Commit local state
        setGroupCheckpoints(next);

        // If we just completed the last checkpoint, mark group as completed in backend
        if (isPassing && allCompleted) {
            try {
                const res = await fetch(`http://localhost:8080/lti/labs/${labId}/groups/${selectedGroup.groupId}/pass`, {
                    method: 'POST'
                });
                if (!res.ok) {
                    console.error('Failed to update group status to completed');
                } else {
                    // Update local groups list to reflect completed status
                    setGroups(prev => prev.map(g => (
                        g.id === selectedGroup.id ? { ...g, status: 'completed' } : g
                    )));
                }
            } catch (e) {
                console.error('Error updating group status:', e);
            }
        }

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

    const handleUpdateGroups = (updatedGroups) => {
        // Update the groups state with the modified groups data
        setGroups(updatedGroups);
        console.log('Groups updated successfully!', updatedGroups);
    };

    // Add loading and error states to the render
    if (loading) {
        return (
            <>
                <Header />
                <main className="checkpoint-shell">
                    <div style={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        height: '50vh',
                        fontSize: '18px',
                        color: '#64748b'
                    }}>
                        Loading...
                    </div>
                </main>
            </>
        );
    }

    if (error) {
        return (
            <>
                <Header />
                <main className="checkpoint-shell">
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
                        <div>Error loading data: {error}</div>
                        <button
                            onClick={() => navigate('/lab-selector')}
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
        );
    }

    return (
        <>
            <Header />
            <main className="checkpoint-shell">
                {/* Header with breadcrumb and actions */}
                <div className="checkpoint-page-header">
                    <div className="checkpoint-nav">
                        <button className="breadcrumb-back" onClick={() => navigate('/lab-selector')}>
                            <span className="back-arrow">←</span>
                            <span>Back to Labs</span>
                        </button>
                        <span className="breadcrumb-separator">/</span>
                        <h1 className="checkpoint-title">{currentLab}</h1>
                        {group && <span className="checkpoint-subtitle"> - {group.groupId}</span>}
                    </div>
                    <div className="checkpoint-actions">
                        <button className="action-btn secondary" onClick={handleEditGroups}>
                            <span className="btn-icon">✏️</span>
                            Manage Groups
                        </button>
                    </div>
                </div>

                {/* Groups Panel - Connected to real database */}
                <section className="groups-panel" aria-labelledby="groups-title">
                    <header className="panel-header">
                        <h2 id="groups-title" className="panel-title">
                            Groups
                        </h2>
                        <span className="groups-count">{groups.length} group{groups.length !== 1 ? 's' : ''}</span>
                    </header>

                    <div className="groups-list">
                        {groups.length === 0 ? (
                            <div className="no-groups" style={{
                                padding: '40px 20px',
                                textAlign: 'center',
                                color: '#64748b',
                                fontSize: '16px'
                            }}>
                                No groups available for this lab
                            </div>
                        ) : (
                            groups.map(group => {
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
                                            <h3 className="group-name">{group.groupId}</h3>
                                            <span className={`group-status ${group.status.toLowerCase().replace(' ', '-')}`}>
                                                {group.status}
                                            </span>
                                        </div>

                                        <div className="group-card-body">
                                            <div className="group-members">
                                                <div className="members-list">
                                                    {group.members.map((member, index) => (
                                                        <span key={index} className="member-name">
                                                            {member}{index < group.members.length - 1 ? ', ' : ''}
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
                            })
                        )}
                    </div>
                </section>

                {/* Checkpoint List Panel - Now below groups */}
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
                    groupsData={groups}
                    labId={labId}
                    onUpdateGroups={handleUpdateGroups}
                />
            </main>
        </>
    );
}
