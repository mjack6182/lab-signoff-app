import { useState, useEffect } from 'react';
import './GroupManagementModal.css';
import {
    fetchEnrolledStudents,
    fetchLabGroups,
    randomizeGroups as randomizeGroupsAPI,
    updateGroups as updateGroupsAPI,
    calculateUnassignedStudents
} from '../../services/groupService';

export default function GroupManagementModal({
    isOpen,
    onClose,
    labId,
    classId,
    labName,
    onUpdateGroups
}) {
    const [unassignedStudents, setUnassignedStudents] = useState([]);
    const [groups, setGroups] = useState([]);
    const [draggedItem, setDraggedItem] = useState(null);
    const [draggedFrom, setDraggedFrom] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [showRandomizeConfirm, setShowRandomizeConfirm] = useState(false);

    // Fetch data when modal opens
    useEffect(() => {
        if (isOpen && labId && classId) {
            fetchData();
        }
    }, [isOpen, labId, classId]);

    const fetchData = async () => {
        setLoading(true);
        setError(null);

        try {
            // Fetch enrolled students and existing groups in parallel
            const [studentsData, groupsData] = await Promise.all([
                fetchEnrolledStudents(classId),
                fetchLabGroups(labId)
            ]);

            // Transform enrolled students to match the expected format
            const students = studentsData.map(enrollment => ({
                id: enrollment.userId,
                userId: enrollment.userId,
                name: enrollment.userName || enrollment.userEmail || 'Unknown Student',
                email: enrollment.userEmail || '',
                firstName: enrollment.userName?.split(' ')[0] || ''
            }));

            // Transform groups to match the expected format
            const transformedGroups = groupsData.map(group => ({
                id: group.id || group.groupId,
                groupId: group.groupId,
                name: group.groupId || `Group ${group.groupNumber}`,
                members: (group.members || []).map(member => ({
                    id: member.userId,
                    userId: member.userId,
                    name: member.name,
                    email: member.email,
                    firstName: member.name?.split(' ')[0] || ''
                })),
                status: group.status || 'FORMING',
                checkpointProgress: group.checkpointProgress || 0,
                groupNumber: group.groupNumber
            }));

            // Calculate unassigned students
            const unassigned = calculateUnassignedStudents(students, transformedGroups);

            setGroups(transformedGroups);
            setUnassignedStudents(unassigned);
        } catch (err) {
            console.error('Failed to fetch group data:', err);
            setError(err.message || 'Failed to load group data');
        } finally {
            setLoading(false);
        }
    };

    const handleDragStart = (e, student, fromType, fromId = null) => {
        setDraggedItem(student);
        setDraggedFrom({ type: fromType, id: fromId });
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleDragOver = (e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    };

    // Helper function to get first name from student object
    const getFirstName = (student) => {
        // Prefer firstName field if available, otherwise fall back to parsing name
        return student.firstName || student.name?.split(' ')[0] || 'Unknown';
    };

    // Helper function to generate group name from student first names
    const generateGroupName = (members) => {
        if (members.length === 0) return 'Empty Group';
        if (members.length === 1) return getFirstName(members[0]);
        if (members.length === 2) {
            const firstName1 = getFirstName(members[0]);
            const firstName2 = getFirstName(members[1]);
            return `${firstName1} & ${firstName2}`;
        }
        // For 3+ members, show first two names + "& X more"
        const firstName1 = getFirstName(members[0]);
        const firstName2 = getFirstName(members[1]);
        const remainingCount = members.length - 2;
        return `${firstName1} & ${firstName2} & ${remainingCount} more`;
    };

    const handleDrop = (e, toType, toId = null) => {
        e.preventDefault();
        
        if (!draggedItem || !draggedFrom) return;

        // Don't do anything if dropping in the same place
        if (draggedFrom.type === toType && draggedFrom.id === toId) {
            setDraggedItem(null);
            setDraggedFrom(null);
            return;
        }

        // Remove student from source and update source group name
        if (draggedFrom.type === 'unassigned') {
            setUnassignedStudents(prev => 
                prev.filter(student => student.id !== draggedItem.id)
            );
        } else if (draggedFrom.type === 'group') {
            setGroups(prev => 
                prev.map(group => {
                    if (group.id === draggedFrom.id) {
                        const updatedMembers = group.members.filter(m => m.id !== draggedItem.id);
                        return { 
                            ...group, 
                            members: updatedMembers,
                            name: generateGroupName(updatedMembers)
                        };
                    }
                    return group;
                })
            );
        }

        // Add student to destination and update destination group name
        if (toType === 'unassigned') {
            setUnassignedStudents(prev => [...prev, draggedItem]);
        } else if (toType === 'group') {
            setGroups(prev => 
                prev.map(group => {
                    if (group.id === toId) {
                        const updatedMembers = [...group.members, draggedItem];
                        return { 
                            ...group, 
                            members: updatedMembers,
                            name: generateGroupName(updatedMembers)
                        };
                    }
                    return group;
                })
            );
        }

        setDraggedItem(null);
        setDraggedFrom(null);
    };

    const handleAddGroup = () => {
        const newGroupNumber = groups.length + 1;
        const newGroup = {
            id: `new-group-${Date.now()}`,
            name: `Group ${newGroupNumber}`,
            members: [],
            status: 'Active',
            checkpointProgress: 0
        };
        setGroups(prev => [...prev, newGroup]);
    };

    const handleDeleteGroup = (groupId) => {
        const groupToDelete = groups.find(g => g.id === groupId);
        if (groupToDelete && groupToDelete.members.length > 0) {
            // Move students to unassigned
            setUnassignedStudents(prev => [...prev, ...groupToDelete.members]);
        }
        // Remove the group
        setGroups(prev => prev.filter(g => g.id !== groupId));
    };

    const handleRandomize = () => {
        // Show confirmation if groups already exist
        if (groups.length > 0) {
            setShowRandomizeConfirm(true);
        } else {
            executeRandomize();
        }
    };

    const executeRandomize = async () => {
        setShowRandomizeConfirm(false);
        setLoading(true);
        setError(null);

        try {
            const response = await randomizeGroupsAPI(labId);

            // Transform the response groups to match our format
            const transformedGroups = response.groups.map(group => ({
                id: group.id || group.groupId,
                groupId: group.groupId,
                name: group.groupId || `Group ${group.groupNumber}`,
                members: (group.members || []).map(member => ({
                    id: member.userId,
                    userId: member.userId,
                    name: member.name,
                    email: member.email,
                    firstName: member.name?.split(' ')[0] || ''
                })),
                status: group.status || 'FORMING',
                checkpointProgress: group.checkpointProgress || 0,
                groupNumber: group.groupNumber
            }));

            setGroups(transformedGroups);
            setUnassignedStudents([]); // All students are now assigned

            // Notify parent component
            if (onUpdateGroups) {
                onUpdateGroups();
            }
        } catch (err) {
            console.error('Failed to randomize groups:', err);
            setError(err.message || 'Failed to randomize groups');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setLoading(true);
        setError(null);

        try {
            // Filter out empty groups before saving
            const nonEmptyGroups = groups.filter(group => group.members && group.members.length > 0);

            // Transform groups to backend format
            const groupsToSave = nonEmptyGroups.map((group, index) => ({
                groupId: group.groupId || `Group-${index + 1}`,
                groupNumber: group.groupNumber || (index + 1),
                labId: labId,
                members: group.members.map(member => ({
                    userId: member.userId || member.id,
                    name: member.name,
                    email: member.email,
                    present: true
                })),
                status: group.status || 'FORMING'
            }));

            await updateGroupsAPI(labId, groupsToSave);

            // Notify parent component to refresh
            if (onUpdateGroups) {
                onUpdateGroups();
            }

            onClose();
        } catch (err) {
            console.error('Failed to save groups:', err);
            setError(err.message || 'Failed to save groups');
            setLoading(false);
        }
    };

    const handleCancel = () => {
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="group-management-overlay">
            <div className="group-management-modal">
                <div className="modal-header">
                    <h2 className="modal-title">Manage Groups - {labName || 'Lab'}</h2>
                    <div className="header-actions">
                        <button
                            className="btn-randomize"
                            onClick={handleRandomize}
                            disabled={loading || unassignedStudents.length === 0}
                            title={unassignedStudents.length === 0 ? "No students to randomize" : "Randomize all students into groups"}
                        >
                            🎲 Randomize Groups
                        </button>
                        <button className="modal-close" onClick={onClose}>×</button>
                    </div>
                </div>

                {error && (
                    <div className="error-banner">
                        <span className="error-icon">⚠️</span>
                        <span className="error-message">{error}</span>
                        <button className="error-close" onClick={() => setError(null)}>×</button>
                    </div>
                )}

                {loading && (
                    <div className="loading-overlay">
                        <div className="loading-spinner"></div>
                        <p>Loading...</p>
                    </div>
                )}

                {showRandomizeConfirm && (
                    <div className="confirm-dialog-overlay">
                        <div className="confirm-dialog">
                            <h3>Confirm Randomization</h3>
                            <p>This will replace all existing groups with new randomized groups. Are you sure you want to continue?</p>
                            <div className="confirm-actions">
                                <button className="btn-cancel" onClick={() => setShowRandomizeConfirm(false)}>
                                    Cancel
                                </button>
                                <button className="btn-confirm" onClick={executeRandomize}>
                                    Yes, Randomize
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <div className="modal-body">
                    {/* Unassigned Students Section */}
                    <div className="unassigned-section">
                        <div className="section-header">
                            <h3 className="section-title">Unassigned Students</h3>
                            <span className="student-count">{unassignedStudents.length} students</span>
                        </div>
                        <div 
                            className="students-grid unassigned-grid"
                            onDragOver={handleDragOver}
                            onDrop={(e) => handleDrop(e, 'unassigned')}
                        >
                            {unassignedStudents.map(student => (
                                <div
                                    key={student.id}
                                    className="student-card"
                                    draggable
                                    onDragStart={(e) => handleDragStart(e, student, 'unassigned')}
                                >
                                    <span className="student-name">{student.name}</span>
                                </div>
                            ))}
                            {unassignedStudents.length === 0 && (
                                <div className="empty-message">All students are assigned to groups</div>
                            )}
                        </div>
                    </div>

                    {/* Groups Section */}
                    <div className="groups-section">
                        <div className="section-header">
                            <h3 className="section-title">Groups</h3>
                            <button className="add-group-btn" onClick={handleAddGroup}>
                                <span className="plus-icon">+</span>
                                Add Group
                            </button>
                        </div>
                        
                        <div className="groups-grid">
                            {groups.map(group => (
                                <div key={group.id} className="group-container">
                                    <div className="group-header">
                                        <h4 className="group-name">{group.name}</h4>
                                        <div className="group-actions">
                                            <span className="member-count">{group.members.length}</span>
                                            <button 
                                                className="delete-group-btn"
                                                onClick={() => handleDeleteGroup(group.id)}
                                                title="Delete Group"
                                            >
                                                ×
                                            </button>
                                        </div>
                                    </div>
                                    <div 
                                        className="group-members"
                                        onDragOver={handleDragOver}
                                        onDrop={(e) => handleDrop(e, 'group', group.id)}
                                    >
                                        {group.members.map(member => (
                                            <div
                                                key={member.id}
                                                className="student-card group-member"
                                                draggable
                                                onDragStart={(e) => handleDragStart(e, member, 'group', group.id)}
                                            >
                                                <span className="student-name">{member.name}</span>
                                            </div>
                                        ))}
                                        {group.members.length === 0 && (
                                            <div className="empty-group">Drop students here</div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="btn-cancel" onClick={handleCancel}>
                        Cancel
                    </button>
                    <button className="btn-save" onClick={handleSave}>
                        Save Changes
                    </button>
                </div>
            </div>
        </div>
    );
}