import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mockCheckpoints } from '../../mock/checkpoints';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import './checkpoints.css';

/**
 * CheckpointPage Component (Student View)
 *
 * Displays read-only checkpoints for a specific lab with group selection.
 * Students can view checkpoint progress but cannot modify completion status.
 *
 * URL Parameters:
 * - labId: The ID of the lab (required)
 * - groupId: The ID of the group (optional - for backward compatibility and direct group access)
 *
 * Features:
 * - Auto-selects first group if no groupId is provided
 * - Displays all groups for the lab with progress indicators
 * - Allows switching between groups without navigation
 * - Read-only view of checkpoint completion status
 * - Support for help requests (future feature)
 */
export default function CheckpointPage() {
    const { labId, groupId } = useParams();
    const navigate = useNavigate();
    const [groups, setGroups] = useState([]);
    const [selectedGroupId, setSelectedGroupId] = useState(groupId || null);
    const [showGroupManagement, setShowGroupManagement] = useState(false);
    const [group, setGroup] = useState(null);
    const [lab, setLab] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Mock checkpoint completion data - completely static for student view
    const [groupCheckpoints, setGroupCheckpoints] = useState({});

    const selectedGroup = groups.find(g => g.id === selectedGroupId);
    const currentLab = lab?.courseId || "Lab"; // Use actual lab data

    // Load mock data when component mounts - no backend connections
    useEffect(() => {
        if (labId) {
            setLoading(true);

            // Pure mock data loading for student demo - no API calls
            const loadMockData = () => {
                console.log('Loading mock data for student view - no backend');

                // Mock lab data
                const mockLab = { courseId: `CS101 - Lab ${labId}` };
                setLab(mockLab);

                // Mock groups data with realistic information
                const mockGroups = [
                    {
                        id: groupId || 'group-1',
                        groupId: groupId || 'Group-8',
                        status: 'in-progress',
                        members: ['Student A', 'Student B', 'Student C'],
                        labId: labId
                    },
                    {
                        id: 'group-2',
                        groupId: 'Group-7',
                        status: 'completed',
                        members: ['Student D', 'Student E'],
                        labId: labId
                    },
                    {
                        id: 'group-3',
                        groupId: 'Group-6',
                        status: 'pending',
                        members: ['Student F', 'Student G', 'Student H', 'Student I'],
                        labId: labId
                    }
                ];

                setGroups(mockGroups);

                // Set selected group
                if (groupId) {
                    const foundGroup = mockGroups.find(g => g.groupId === groupId || g.id === groupId);
                    setGroup(foundGroup || mockGroups[0]);
                    setSelectedGroupId(foundGroup?.id || mockGroups[0].id);
                } else {
                    setSelectedGroupId(mockGroups[0].id);
                    setGroup(mockGroups[0]);
                }

                // Initialize mock checkpoint completion data with realistic progress
                const initial = {};
                mockGroups.forEach(group => {
                    initial[group.id] = {};
                    if (mockCheckpoints.length > 0) {
                        // Group 1: First checkpoint completed
                        if (group.id === (groupId || 'group-1')) {
                            initial[group.id][mockCheckpoints[0].id] = {
                                completed: true,
                                completedAt: '2024-10-25',
                                completedBy: 'instructor',
                                notes: 'Completed successfully'
                            };
                        }

                        // Group 2: More progress (completed group)
                        if (group.id === 'group-2' && mockCheckpoints.length > 1) {
                            initial[group.id][mockCheckpoints[0].id] = {
                                completed: true,
                                completedAt: '2024-10-25',
                                completedBy: 'instructor',
                                notes: 'Completed successfully'
                            };
                            initial[group.id][mockCheckpoints[1].id] = {
                                completed: true,
                                completedAt: '2024-10-26',
                                completedBy: 'instructor',
                                notes: 'Good work'
                            };
                            if (mockCheckpoints.length > 2) {
                                initial[group.id][mockCheckpoints[2].id] = {
                                    completed: true,
                                    completedAt: '2024-10-27',
                                    completedBy: 'instructor',
                                    notes: 'Excellent progress'
                                };
                            }
                        }

                        // Group 3: No progress (pending group)
                        // Leave empty to show pending state
                    }
                });
                setGroupCheckpoints(initial);

                setError(null);
                setLoading(false);
            };

            // Load mock data immediately for fast student experience
            setTimeout(loadMockData, 100); // Very short delay to show loading briefly
        }
    }, [labId, groupId]);    // Update selected group when selectedGroupId changes
    useEffect(() => {
        if (selectedGroupId && groups.length > 0) {
            const foundGroup = groups.find(g => g.id === selectedGroupId);
            setGroup(foundGroup);
        }
    }, [selectedGroupId, groups]);

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
                            onClick={() => navigate('/lab-join')}
                            style={{
                                padding: '8px 16px',
                                fontSize: '14px',
                                cursor: 'pointer'
                            }}
                        >
                            Back to Lab Join
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
                        <button className="breadcrumb-back" onClick={() => navigate('/lab-join')}>
                            <span className="back-arrow">←</span>
                            <span>Back to Lab Join</span>
                        </button>
                        <span className="breadcrumb-separator">/</span>
                        <h1 className="checkpoint-title">{currentLab}</h1>
                        {group && <span className="checkpoint-subtitle"> - {group.groupId}</span>}
                    </div>
                </div>

                {/* Main Content Grid - Checkpoints take up most space, sidebar on right */}
                <div className="checkpoint-content">
                    {/* Checkpoint List Panel - Takes up most of the screen */}
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
                                                    <span className={`checkpoint-status ${isCompleted ? 'completed' : 'pending'}`}>
                                                        {isCompleted ? 'Completed' : 'Pending'}
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </section>

                    {/* Right Sidebar - Student Group Info */}
                    <aside className="student-sidebar">
                        {/* Student Group Info */}
                        {selectedGroup && (
                            <section className="student-group-panel">
                                <header className="panel-header">
                                    <h2 className="panel-title">Your Group</h2>
                                </header>

                                <div className="student-group-info">
                                    <div className="group-card selected">
                                        <div className="group-card-header">
                                            <h3 className="group-name">{selectedGroup.groupId}</h3>
                                        </div>

                                        <div className="group-card-body">
                                            <div className="group-members">
                                                <div className="members-title">Members:</div>
                                                <div className="members-list">
                                                    {selectedGroup.members.map((member, index) => (
                                                        <div key={index} className="member-item">
                                                            <span className="member-name">{member}</span>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>

                                            <div className="group-progress">
                                                <div className="progress-header">
                                                    <span className="progress-text">Progress</span>
                                                    <span className="progress-value">
                                                        {getCompletedCount(selectedGroup.id)}/{mockCheckpoints.length}
                                                    </span>
                                                </div>
                                                <div className="progress-bar">
                                                    <div
                                                        className="progress-fill"
                                                        style={{
                                                            width: `${Math.round((getCompletedCount(selectedGroup.id) / mockCheckpoints.length) * 100)}%`
                                                        }}
                                                    ></div>
                                                </div>
                                                <div className="progress-percentage">
                                                    {Math.round((getCompletedCount(selectedGroup.id) / mockCheckpoints.length) * 100)}% Complete
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>
                        )}

                        {/* Help Section */}
                        <section className="help-section">
                            <button
                                className="help-btn"
                                onClick={() => alert('Help request sent to instructor!')}
                            >
                                <span className="help-text">Request Help</span>
                            </button>
                        </section>
                    </aside>
                </div>

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