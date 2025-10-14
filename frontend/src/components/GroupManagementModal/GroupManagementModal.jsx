import { useState, useEffect } from 'react';
import './GroupManagementModal.css';

export default function GroupManagementModal({ 
    isOpen, 
    onClose, 
    sectionData, 
    onUpdateGroups 
}) {
    const [unassignedStudents, setUnassignedStudents] = useState([]);
    const [groups, setGroups] = useState([]);
    const [draggedItem, setDraggedItem] = useState(null);
    const [draggedFrom, setDraggedFrom] = useState(null);

    // Initialize data when modal opens
    useEffect(() => {
        if (isOpen && sectionData) {
            // Get all students from groups
            const allStudents = [];
            const currentGroups = sectionData.groups?.map(group => ({
                ...group,
                members: [...(group.members || [])]
            })) || [];

            currentGroups.forEach(group => {
                allStudents.push(...group.members);
            });

            // Get unassigned students from sectionData (if provided) or initialize as empty
            const unassigned = sectionData.unassignedStudents || [];

            setGroups(currentGroups);
            setUnassignedStudents(unassigned);
        }
    }, [isOpen, sectionData]);

    const handleDragStart = (e, student, fromType, fromId = null) => {
        setDraggedItem(student);
        setDraggedFrom({ type: fromType, id: fromId });
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleDragOver = (e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    };

    // Helper function to generate group name from student first names
    const generateGroupName = (members) => {
        if (members.length === 0) return 'Empty Group';
        if (members.length === 1) return members[0].name.split(' ')[0];
        if (members.length === 2) {
            const firstName1 = members[0].name.split(' ')[0];
            const firstName2 = members[1].name.split(' ')[0];
            return `${firstName1} & ${firstName2}`;
        }
        // For 3+ members, show first two names + "& X more"
        const firstName1 = members[0].name.split(' ')[0];
        const firstName2 = members[1].name.split(' ')[0];
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

    const handleSave = () => {
        // Filter out empty groups before saving
        const nonEmptyGroups = groups.filter(group => group.members && group.members.length > 0);
        
        // Create updated section data with non-empty groups and preserve unassigned students
        const updatedSectionData = {
            ...sectionData,
            groups: nonEmptyGroups,
            unassignedStudents: unassignedStudents // Preserve unassigned students
        };
        
        console.log(`Removed ${groups.length - nonEmptyGroups.length} empty groups`);
        console.log(`Preserved ${unassignedStudents.length} unassigned students`);
        
        onUpdateGroups(updatedSectionData);
        onClose();
    };

    const handleCancel = () => {
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="group-management-overlay">
            <div className="group-management-modal">
                <div className="modal-header">
                    <h2 className="modal-title">Manage Groups - {sectionData?.name}</h2>
                    <button className="modal-close" onClick={onClose}>×</button>
                </div>

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