import { useState } from 'react';
import CheckpointCard from '../components/CheckpointCard';
import { groups } from '../mock/groups';

export default function CheckpointPage() {
    const [groupsData, setGroupsData] = useState(groups);
    const [selectedGroupId, setSelectedGroupId] = useState(groups[0]?.id || null);
    
    const selectedGroup = groupsData.find(g => g.id === selectedGroupId);
    
    const handleCheckpointToggle = (groupId, checkpointId, updatedCheckpoint) => {
        setGroupsData(prevGroups => 
            prevGroups.map(group => 
                group.id === groupId 
                    ? {
                        ...group,
                        checkpoints: group.checkpoints.map(cp => 
                            cp.id === checkpointId ? updatedCheckpoint : cp
                        ),
                        updatedAt: new Date().toISOString().split('T')[0]
                    }
                    : group
            )
        );
    };
    
    return (
        <main className="checkpoint-page">
            <header className="checkpoint-page-header">
                <h1 className="page-title">Lab Checkpoint Manager</h1>
                <p className="page-subtitle">
                    Track and manage student progress through lab checkpoints
                </p>
            </header>
            
            <div className="checkpoint-page-content">
                <aside className="group-selector">
                    <h2 className="selector-title">Select Group</h2>
                    <div className="group-list">
                        {groupsData.map(group => {
                            const completedCount = group.checkpoints.filter(cp => cp.completed).length;
                            const totalCount = group.checkpoints.length;
                            
                            return (
                                <button
                                    key={group.id}
                                    onClick={() => setSelectedGroupId(group.id)}
                                    className={`group-selector-item ${group.id === selectedGroupId ? 'active' : ''}`}
                                    aria-pressed={group.id === selectedGroupId}
                                >
                                    <div className="group-info">
                                        <h3 className="group-name">{group.name}</h3>
                                        <div className="group-details">
                                            <span className="group-course">{group.course}</span>
                                            <span className="group-progress">
                                                {completedCount}/{totalCount} complete
                                            </span>
                                        </div>
                                    </div>
                                    <span className={`group-status-indicator ${group.status.toLowerCase()}`}>
                                        {group.status}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </aside>
                
                <section className="checkpoint-content">
                    {selectedGroup ? (
                        <CheckpointCard
                            groupId={selectedGroup.id}
                            groupName={selectedGroup.name}
                            checkpoints={selectedGroup.checkpoints}
                            members={selectedGroup.members}
                            onCheckpointToggle={handleCheckpointToggle}
                        />
                    ) : (
                        <div className="no-group-selected">
                            <p>Select a group to view and manage checkpoints</p>
                        </div>
                    )}
                </section>
            </div>
        </main>
    );
}