import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';

export default function CheckpointCard({ 
    groupId, 
    groupName, 
    checkpoints = [], 
    members = [], 
    onCheckpointToggle
}) {
    const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
    const { user, isTeacherOrTA } = useAuth();
    
    const completedCount = checkpoints.filter(cp => cp.completed).length;
    const totalCount = checkpoints.length;
    const progressPercentage = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;
    
    const getCompletedByName = (userId) => {
        const member = members.find(m => m.id === userId);
        if (member) return member.name;
        
        // If not found in members, check if it's the current user
        if (user && userId === user.id) return user.name;
        
        return 'Unknown';
    };
    
    const handleCheckpointToggle = (checkpointId, isCompleted) => {
        if (!isTeacherOrTA) return;
        
        const checkpoint = checkpoints.find(cp => cp.id === checkpointId);
        if (!checkpoint) return;
        
        const updatedCheckpoint = {
            ...checkpoint,
            completed: isCompleted,
            completedAt: isCompleted ? new Date().toISOString().split('T')[0] : null,
            completedBy: isCompleted ? user.id : null
        };
        
        onCheckpointToggle && onCheckpointToggle(groupId, checkpointId, updatedCheckpoint);
    };
    
    return (
        <div className="checkpoint-card">
            <header className="checkpoint-header">
                <div>
                    <h3 className="checkpoint-title">{groupName}</h3>
                    <div className="checkpoint-progress-summary">
                        <span className="progress-text">{completedCount}/{totalCount} checkpoints complete</span>
                        <span className="progress-percentage">({progressPercentage}%)</span>
                    </div>
                </div>
                {!isTeacherOrTA && (
                    <div className="readonly-badge">View Only</div>
                )}
            </header>
            
            <div className="checkpoint-progress-bar">
                <div 
                    className="progress-fill" 
                    style={{ width: `${progressPercentage}%` }}
                    aria-label={`${progressPercentage}% complete`}
                />
            </div>
            
            <div className="checkpoint-list">
                {checkpoints.map((checkpoint, index) => (
                    <div 
                        key={checkpoint.id} 
                        className={`checkpoint-item ${checkpoint.completed ? 'completed' : 'pending'}`}
                    >
                        <div className="checkpoint-main">
                            <div className="checkpoint-indicator">
                                <input
                                    type="checkbox"
                                    id={`checkpoint-${checkpoint.id}`}
                                    checked={checkpoint.completed}
                                    onChange={(e) => handleCheckpointToggle(checkpoint.id, e.target.checked)}
                                    disabled={!isTeacherOrTA}
                                    className="checkpoint-checkbox"
                                    aria-describedby={`checkpoint-${checkpoint.id}-details`}
                                />
                                <span className="checkpoint-number">{index + 1}</span>
                            </div>
                            
                            <div className="checkpoint-content">
                                <label 
                                    htmlFor={`checkpoint-${checkpoint.id}`}
                                    className="checkpoint-name"
                                >
                                    {checkpoint.name}
                                </label>
                                
                                {checkpoint.completed && (
                                    <div 
                                        id={`checkpoint-${checkpoint.id}-details`}
                                        className="checkpoint-details"
                                    >
                                        <span className="completed-by">
                                            ✓ Completed by {getCompletedByName(checkpoint.completedBy)}
                                        </span>
                                        <span className="completed-date">
                                            on {new Date(checkpoint.completedAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                )}
                            </div>
                        </div>
                        
                        {isTeacherOrTA && checkpoint.completed && (
                            <button
                                onClick={() => handleCheckpointToggle(checkpoint.id, false)}
                                className="undo-button"
                                title="Mark as incomplete"
                                aria-label={`Mark ${checkpoint.name} as incomplete`}
                            >
                                ↶
                            </button>
                        )}
                    </div>
                ))}
            </div>
            
            {checkpoints.length === 0 && (
                <div className="no-checkpoints">
                    No checkpoints defined for this group.
                </div>
            )}
        </div>
    );
}