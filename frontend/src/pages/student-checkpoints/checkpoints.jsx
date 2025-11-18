import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { mockCheckpoints } from '../../mock/checkpoints';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import { api } from '../../config/api';
import './checkpoints.css';

const normalizeCheckpoints = (definitions = []) => {
    const isPlaceholderCheckpoint = (definition) => {
        if (!definition || !definition.name) return true;
        const placeholderName = /^checkpoint\s+\d+$/i.test(definition.name.trim());
        const description = definition.description || '';
        const placeholderDescription = description.trim().toLowerCase()
            .startsWith('complete checkpoint');
        return placeholderName && placeholderDescription;
    };

    const shouldUseMock = !Array.isArray(definitions) ||
        definitions.length === 0 ||
        definitions.every(isPlaceholderCheckpoint);

    const source = shouldUseMock ? mockCheckpoints : definitions;

    return source.map((checkpoint, index) => {
        const number = checkpoint.number ?? checkpoint.order ?? index + 1;
        return {
            ...checkpoint,
            id: checkpoint.id || `checkpoint-${number}`,
            number,
            order: number,
            name: checkpoint.name || `Checkpoint ${number}`,
            description: checkpoint.description || 'Complete this checkpoint',
            points: checkpoint.points || 1
        };
    }).sort((a, b) => (a.order || 0) - (b.order || 0));
};

const buildCheckpointMap = (groupEntity, checkpointDefs = []) => {
    if (!groupEntity || !groupEntity.id) {
        return {};
    }

    const definitions = checkpointDefs.length > 0 ? checkpointDefs : normalizeCheckpoints();
    const progressEntries = {};

    (groupEntity.checkpointProgress || []).forEach(progressItem => {
        const definition = definitions.find(def => def.number === progressItem.checkpointNumber);
        const checkpointId = definition
            ? definition.id
            : `checkpoint-${progressItem.checkpointNumber}`;
        const status = (progressItem.status || '').toString().toUpperCase();
        const completed = status === 'PASS' || status === 'COMPLETE';

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
    const { labId, groupId: routeGroupId } = useParams();
    const navigate = useNavigate();
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

    const initialLab = stateLabData || null;
    const initialGroup = stateGroupData || null;
    const initialGroupId = routeGroupId || initialGroup?.id || stateGroupId || null;

    const [groups, setGroups] = useState(initialGroup ? [initialGroup] : []);
    const [selectedGroupId, setSelectedGroupId] = useState(initialGroupId);
    const [showGroupManagement, setShowGroupManagement] = useState(false);
    const [group, setGroup] = useState(initialGroup);
    const [lab, setLab] = useState(initialLab);
    const [labCheckpoints, setLabCheckpoints] = useState(
        stateLabCheckpoints || initialLab?.checkpoints || []
    );
    const [loading, setLoading] = useState(!(initialLab && initialGroupId));
    const [error, setError] = useState(null);

    const normalizedCheckpoints = useMemo(
        () => normalizeCheckpoints(labCheckpoints),
        [labCheckpoints]
    );
    const [groupCheckpoints, setGroupCheckpoints] = useState(() => {
        if (initialGroup && initialGroup.id) {
            const initialDefs = normalizeCheckpoints(
                stateLabCheckpoints || initialLab?.checkpoints || []
            );
            return {
                [initialGroup.id]: buildCheckpointMap(initialGroup, initialDefs)
            };
        }
        return {};
    });

    const selectedGroup = groups.find(g => g.id === selectedGroupId) ||
        groups.find(g => g.groupId === selectedGroupId || g.groupId === stateGroupDisplayId);
    const currentLab = lab?.title || lab?.courseId || stateLabTitle || stateLabCode || "Lab";
    const checkpoints = normalizedCheckpoints;
    const totalCheckpoints = checkpoints.length;

    // Load real lab/group data when component mounts
    useEffect(() => {
        if (!labId || !initialGroupId) {
            setError('Missing lab or group information');
            setLoading(false);
            return;
        }

        let isActive = true;
        const targetGroupId = routeGroupId || initialGroupId;
        setLoading(true);

        const fetchLabPromise = lab && lab.checkpoints?.length
            ? Promise.resolve(lab)
            : fetch(api.labDetail(labId)).then(res => {
                if (!res.ok) {
                    throw new Error('Failed to load lab information');
                }
                return res.json();
            });

        const fetchGroupPromise = fetch(api.labGroupDetail(labId, targetGroupId))
            .then(res => {
                if (!res.ok) {
                    throw new Error('Failed to load group information');
                }
                return res.json();
            });

        Promise.all([fetchLabPromise, fetchGroupPromise])
            .then(([labData, groupData]) => {
                if (!isActive) {
                    return;
                }

                if (labData) {
                    setLab(labData);
                    setLabCheckpoints(labData.checkpoints || []);
                }

                if (groupData) {
                    setGroup(groupData);
                    setGroups([groupData]);
                    setSelectedGroupId(groupData.id);
                }

                setError(null);
            })
            .catch(err => {
                if (isActive) {
                    setError(err.message || 'Failed to load lab information');
                }
            })
            .finally(() => {
                if (isActive) {
                    setLoading(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [labId, routeGroupId, initialGroupId]);

    // Update selected group when selectedGroupId changes
    useEffect(() => {
        if (selectedGroupId && groups.length > 0) {
            const foundGroup = groups.find(g => g.id === selectedGroupId) ||
                groups.find(g => g.groupId === selectedGroupId);
            if (foundGroup) {
                setGroup(foundGroup);
            }
        }
    }, [selectedGroupId, groups]);

    useEffect(() => {
        if (group && group.id) {
            setGroupCheckpoints(prev => ({
                ...prev,
                [group.id]: buildCheckpointMap(group, normalizedCheckpoints)
            }));
        }
    }, [group, normalizedCheckpoints]);

    // Helper function to check if a checkpoint is completed for the selected group
    const isCheckpointCompleted = (checkpointId) => {
        if (!selectedGroup) return false;
        return groupCheckpoints[selectedGroup.id]?.[checkpointId]?.completed || false;
    };

    // Helper function to get completed checkpoints count for a group
    const getCompletedCount = (groupId) => {
        if (!groupId || !groupCheckpoints[groupId]) return 0;
        return Object.values(groupCheckpoints[groupId]).filter(cp => cp.completed).length;
    };

    const getCompletionPercent = (groupId) => {
        if (!groupId || totalCheckpoints === 0) return 0;
        return Math.round((getCompletedCount(groupId) / totalCheckpoints) * 100);
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
                                {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{totalCheckpoints}
                            </span>
                        </header>

                        <div className="checkpoint-list">
                            {checkpoints.map((checkpoint, index) => {
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
                                                    : index < checkpoints.findIndex(cp => !isCheckpointCompleted(cp.id))
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
                                                    {(selectedGroup.members || []).map((member, index) => {
                                                        const displayName = typeof member === 'string'
                                                            ? member
                                                            : member.name || member.displayName || member.email || member.userId || `Member ${index + 1}`;
                                                        return (
                                                            <div key={index} className="member-item">
                                                                <span className="member-name">{displayName}</span>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            </div>

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
                                                    {getCompletionPercent(selectedGroup?.id)}% Complete
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