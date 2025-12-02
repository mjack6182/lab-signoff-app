// React + Router hooks
import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';

// UI components + helpers
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import { api } from '../../config/api';
import './checkpoints.css';
import { websocketService } from '../../services/websocketService';

/**
 * Converts a group's checkpoint progress into an easier-to-use map.
 * This lets us check completion status fast by checkpointId (e.g., "cp-1").
 */
const buildCheckpointMap = (groupEntity, checkpointDefs = []) => {
    if (!groupEntity || !groupEntity.id) {
        return {};
    }

    const progressEntries = {};

    // Loop through all progress data the group has
    (groupEntity.checkpointProgress || []).forEach(progressItem => {
        const cpNumber = progressItem.checkpointNumber;
        const checkpointId = `cp-${cpNumber}`;

        // Normalize status into uppercase so it's consistent
        const status = (progressItem.status || '').toString().toUpperCase();

        // Determine whether the checkpoint counts as completed
        const completed =
            status === 'PASS' ||
            status === 'COMPLETE' ||
            status === 'SIGNED_OFF';

        // Store progress info for this checkpoint
        progressEntries[checkpointId] = {
            completed,
            completedAt: progressItem.timestamp
                ? new Date(progressItem.timestamp).toLocaleDateString()
                : null,
            completedBy: progressItem.signedOffByName,
            notes: progressItem.notes
        };
    });

    return progressEntries;
};

/**
 * Student Checkpoint Page
 *
 * This page shows a student:
 * - Their lab
 * - Their group
 * - Their checkpoint progress (read-only)
 *
 * Students cannot change checkpoint status — only view it.
 */
export default function CheckpointPage() {
    // Read route params, e.g., /lab/123/group/5
    const { labId, groupId: routeGroupId } = useParams();

    // Used for going back / redirecting
    const navigate = useNavigate();

    // Data passed from previous page (lab list → checkpoint page)
    const location = useLocation();
    const {
        studentName,
        labCode: stateLabCode,
        labTitle: stateLabTitle,
        className: stateClassName,
        groupId: stateGroupId,
        groupDisplayId: stateGroupDisplayId,
        labData: stateLabData,
        labCheckpoints: stateLabCheckpoints,
        groupData: stateGroupData
    } = location.state || {};

    // Initial values (if passed from previous page)
    const initialLab = stateLabData || null;
    const initialGroup = stateGroupData || null;
    const initialGroupId =
        routeGroupId || initialGroup?.id || stateGroupId || null;

    // Component state
    const [groups, setGroups] = useState(initialGroup ? [initialGroup] : []);
    const [selectedGroupId, setSelectedGroupId] = useState(initialGroupId);
    const [showGroupManagement, setShowGroupManagement] = useState(false);
    const [group, setGroup] = useState(initialGroup);
    const [lab, setLab] = useState(initialLab);
    const [loading, setLoading] = useState(!(initialLab && initialGroupId));
    const [error, setError] = useState(null);

    /**
     * Stores checkpoint progress per group:
     * {
     *   groupId123: {
     *      "cp-1": { completed: true, ... },
     *      "cp-2": { completed: false, ... }
     *   }
     * }
     */
    const [groupCheckpoints, setGroupCheckpoints] = useState(() => {
        if (initialGroup && initialGroup.id) {
            const initialDefs = initialLab?.checkpoints || [];
            return {
                [initialGroup.id]: buildCheckpointMap(initialGroup, initialDefs)
            };
        }
        return {};
    });

    // Find currently selected group
    const selectedGroup =
        groups.find(g => g.id === selectedGroupId) ||
        groups.find(g => g.groupId === selectedGroupId || g.groupId === stateGroupDisplayId);

    // Choose lab title fallback
    const currentLab =
        lab?.title || lab?.courseId || stateLabTitle || stateLabCode || 'Lab';

    // Get checkpoint definitions from the lab
    const checkpoints = lab?.checkpoints || [];

    // Normalize checkpoints into a consistent structure
    const formattedCheckpoints = checkpoints.map(cp => ({
        id: `cp-${cp.number}`,
        name: cp.name,
        description: cp.description,
        points: cp.points,
        order: cp.number,
        number: cp.number
    }));

    const totalCheckpoints = formattedCheckpoints.length;

    /**
     * WebSocket setup — listens for real-time checkpoint updates.
     * When a TA signs off a checkpoint, the student sees it instantly.
     */
    useEffect(() => {
        websocketService.init();
        const groupTopic = '/topic/group-updates';
        const randomizedTopic = `/topic/labs/${labId}/groups-randomized`;

        const refreshGroupsAfterChange = async () => {
            try {
                const res = await fetch(api.labGroups(labId));
                if (!res.ok) return;
                const groupsData = await res.json();
                setGroups(groupsData);

                // Try to keep the student on their group if it still exists
                let nextGroup =
                    groupsData.find(g => g.id === selectedGroupId) ||
                    groupsData.find(g => g.groupId === stateGroupDisplayId || g.groupId === stateGroupId);

                if (!nextGroup && studentName) {
                    const nameLower = studentName.toLowerCase();
                    nextGroup = groupsData.find(g =>
                        (g.members || []).some(m => (m.name || '').toLowerCase() === nameLower)
                    );
                }

                if (nextGroup) {
                    setSelectedGroupId(nextGroup.id);
                } else if (groupsData.length > 0) {
                    setSelectedGroupId(groupsData[0].id);
                }
            } catch (err) {
                console.error('Failed to refresh groups after WS event:', err);
            }
        };

        // Handle connection status
        const statusHandler = (status) => {
            if (status === 'CONNECTED') {
                websocketService.subscribe(groupTopic);
                websocketService.subscribe(randomizedTopic);
            }
        };

        // Handle new messages pushed from server
        const updateHandler = (update, destination) => {
            // Group list changed – refetch
            if (destination && destination.endsWith('/groups-randomized')) {
                refreshGroupsAfterChange();
                return;
            }

            if (!update || !update.groupId) return;

            // Only update if this student's group matches the update
            if (update.groupId !== selectedGroupId) return;

            setGroupCheckpoints(prev => {
                const next = { ...prev };
                if (!next[update.groupId]) next[update.groupId] = {};

                const cpId = `cp-${update.checkpointNumber}`;

                // If checkpoint passed → mark as completed
                if (update.status === 'PASS' || update.status === 'SIGNED_OFF') {
                    next[update.groupId][cpId] = {
                        completed: true,
                        completedAt: update.timestamp || new Date().toISOString(),
                        completedBy:
                            update.signedOffByName ||
                            update.signedOffBy ||
                            'TA'
                    };
                }

                // If checkpoint returned → remove completion
                if (update.status === 'RETURN') {
                    const { [cpId]: removed, ...rest } = next[update.groupId];
                    next[update.groupId] = rest;
                }

                return next;
            });
        };

        // Add listeners
        websocketService.addStatusListener(statusHandler);
        websocketService.addListener(updateHandler);

        // Cleanup when leaving page
        return () => {
            websocketService.removeListener(updateHandler);
            websocketService.removeStatusListener(statusHandler);
            websocketService.unsubscribe(groupTopic);
            websocketService.unsubscribe(randomizedTopic);
            websocketService.disconnect();
        };
    }, [labId, selectedGroupId, studentName, stateGroupDisplayId, stateGroupId]);

    /**
     * Fetch the lab + group data from backend when page loads
     * or when route changes.
     */
    useEffect(() => {
        if (!labId || !initialGroupId) {
            setError('Missing lab or group information');
            setLoading(false);
            return;
        }

        let isActive = true;
        const targetGroupId = routeGroupId || initialGroupId;

        setLoading(true);

        // Fetch latest lab checkpoint definitions
        const fetchLabPromise = fetch(api.labDetail(labId))
            .then(res => {
                if (!res.ok) throw new Error('Failed to load lab information');
                return res.json();
            });

        // Fetch group info
        const fetchGroupPromise = fetch(api.labGroupDetail(labId, targetGroupId))
            .then(res => {
                if (!res.ok) throw new Error('Failed to load group information');
                return res.json();
            });

        // Run both requests at once
        Promise.all([fetchLabPromise, fetchGroupPromise])
            .then(([labData, groupData]) => {
                if (!isActive) return;

                if (labData) setLab(labData);
                if (groupData) {
                    setGroup(groupData);
                    setGroups([groupData]);
                    setSelectedGroupId(groupData.id);
                }

                setError(null);
            })
            .catch(err => {
                if (isActive) setError(err.message || 'Failed to load lab information');
            })
            .finally(() => {
                if (isActive) setLoading(false);
            });

        return () => { isActive = false; };
    }, [labId, routeGroupId, initialGroupId]);

    /**
     * Update selected group whenever selectedGroupId changes.
     */
    useEffect(() => {
        if (selectedGroupId && groups.length > 0) {
            const foundGroup =
                groups.find(g => g.id === selectedGroupId) ||
                groups.find(g => g.groupId === selectedGroupId);
            if (foundGroup) {
                setGroup(foundGroup);
            }
        }
    }, [selectedGroupId, groups]);

    /**
     * Rebuild the progress map whenever lab or group changes.
     * Ensures UI stays synced with updated data.
     */
    useEffect(() => {
        if (group && group.id && lab?.checkpoints) {
            setGroupCheckpoints(prev => ({
                ...prev,
                [group.id]: buildCheckpointMap(group, lab.checkpoints)
            }));
        }
    }, [group, lab]);

    // Helper: is a specific checkpoint completed?
    const isCheckpointCompleted = (checkpointId) => {
        if (!selectedGroup) return false;
        return groupCheckpoints[selectedGroup.id]?.[checkpointId]?.completed || false;
    };

    // Helper: count completed checkpoints
    const getCompletedCount = (groupId) => {
        if (!groupId || !groupCheckpoints[groupId]) return 0;
        return Object.values(groupCheckpoints[groupId]).filter(cp => cp.completed).length;
    };

    // Helper: compute completion % for progress bar
    const getCompletionPercent = (groupId) => {
        if (!groupId || totalCheckpoints === 0) return 0;
        return Math.round((getCompletedCount(groupId) / totalCheckpoints) * 100);
    };

    // Derived values for selected group summary
    const selectedCompletedCount = selectedGroup ? getCompletedCount(selectedGroup.id) : 0;
    const progressPercent = totalCheckpoints
        ? Math.min(100, Math.round((selectedCompletedCount / totalCheckpoints) * 100))
        : 0;

    // Open Group Management
    const handleEditGroups = () => {
        setShowGroupManagement(true);
    };

    // When groups updated from modal
    const handleUpdateGroups = (updatedGroups) => {
        setGroups(updatedGroups);
    };

    const handleHelpRequest = () => {
        alert('Help request sent to instructor!');
    };

    /**
     * LOADING + ERROR UI
     */
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

    /**
     * MAIN PAGE UI
     */
    return (
        <>
            <Header />

            <main className="checkpoint-shell">
                {/* Top breadcrumb + page header */}
                <div className="checkpoint-page-header">
                    <div className="checkpoint-nav">
                        <button
                            className="breadcrumb-back"
                            onClick={() => navigate('/lab-join')}
                        >
                            <span className="back-arrow">←</span>
                            <span>Back to Lab Join</span>
                        </button>

                        <span className="breadcrumb-separator">/</span>
                        <h1 className="checkpoint-title">{currentLab}</h1>

                        {group && (
                            <span className="checkpoint-subtitle"> - {group.groupId}</span>
                        )}
                    </div>
                </div>

                {selectedGroup && (
                    <div className="student-mobile-summary">
                        <div className="student-summary-block">
                            <span className="summary-label">Group</span>
                            <span className="summary-value">{selectedGroup.groupId}</span>
                        </div>
                        <div className="student-summary-block">
                            <span className="summary-label">Progress</span>
                            <span className="summary-value">
                                {selectedCompletedCount}/{totalCheckpoints}
                            </span>
                            <div className="summary-progress-bar">
                                <div className="fill" style={{ width: `${progressPercent}%` }}></div>
                            </div>
                        </div>
                        <div className="student-summary-block">
                            <button className="help-btn condensed" onClick={handleHelpRequest}>
                                Request Help
                            </button>
                        </div>
                    </div>
                )}

                <div className="checkpoint-content">
                    {/* LEFT SIDE — list of checkpoints */}
                    <section className="checkpoint-panel" aria-labelledby="checkpoints-title">
                        <header className="panel-header">
                            <h2 id="checkpoints-title" className="panel-title">
                                Checkpoints
                            </h2>
                            <span className="checkpoint-count">
                                {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{totalCheckpoints}
                            </span>
                        </header>

                        <div className="checkpoint-list">
                            {formattedCheckpoints.map((checkpoint, index) => {
                                const isCompleted = isCheckpointCompleted(checkpoint.id);

                                return (
                                    <div
                                        key={checkpoint.id}
                                        className={`checkpoint-item ${isCompleted ? 'completed' : 'pending'}`}
                                    >
                                        {/* Left: number + progress line */}
                                        <div className="checkpoint-indicator">
                                            <div className="checkpoint-number">
                                                {isCompleted ? '✓' : index + 1}
                                            </div>

                                            {/* Green if completed or past checkpoints */}
                                            <div
                                                className="checkpoint-progress-line"
                                                style={{
                                                    background: isCompleted
                                                        ? '#016836'
                                                        : index < checkpoints.findIndex(cp => !isCheckpointCompleted(cp.id))
                                                            ? '#016836'
                                                            : '#e5e7eb'
                                                }}
                                            ></div>
                                        </div>

                                        {/* Right: main checkpoint info */}
                                        <div className="checkpoint-details">
                                            <div className="checkpoint-main">
                                                <h3 className="checkpoint-name">{checkpoint.name}</h3>
                                                <p className="checkpoint-description">
                                                    {checkpoint.description}
                                                </p>

                                                <div className="checkpoint-meta">
                                                    <span className="checkpoint-points">
                                                        {checkpoint.points} pt
                                                    </span>

                                                    {isCompleted && selectedGroup && (
                                                        <span className="checkpoint-completed-info">
                                                            Completed {groupCheckpoints[selectedGroup.id]?.[checkpoint.id]?.completedAt}
                                                        </span>
                                                    )}
                                                </div>
                                            </div>

                                            {/* Right side status label */}
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

                    {/* RIGHT SIDEBAR — student group info */}
                    <aside className="student-sidebar">
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
                                            {/* Group members */}
                                            <div className="group-members">
                                                <div className="members-title">Members:</div>

                                                <div className="members-list">
                                                    {(selectedGroup.members || []).map((member, index) => {
                                                        const displayName =
                                                            typeof member === 'string'
                                                                ? member
                                                                : member.name ||
                                                                  member.displayName ||
                                                                  member.email ||
                                                                  member.userId ||
                                                                  `Member ${index + 1}`;

                                                        return (
                                                            <div key={index} className="member-item">
                                                                <span className="member-name">
                                                                    {displayName}
                                                                </span>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            </div>

                                            {/* Group progress bar */}
                                            <div className="group-progress">
                                                <div className="progress-header">
                                                    <span className="progress-text">Progress</span>
                                                    <span className="progress-value">
                                                        {getCompletedCount(selectedGroup.id)}/{totalCheckpoints}
                                                    </span>
                                                </div>

                                                <div className="progress-bar">
                                                    <div
                                                        className="progress-fill"
                                                        style={{
                                                            width: `${getCompletionPercent(selectedGroup.id)}%`
                                                        }}
                                                    ></div>
                                                </div>

                                                <div className="progress-percentage">
                                                    {getCompletionPercent(selectedGroup.id)}% Complete
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>
                        )}

                        {/* Request help button */}
                        <section className="help-section">
                            <button
                                className="help-btn"
                                onClick={handleHelpRequest}
                            >
                                <span className="help-text">Request Help</span>
                            </button>
                        </section>
                    </aside>
                </div>

                {/* Group management modal (students normally won't use this) */}
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
