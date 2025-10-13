import React from 'react';
import './SignOffModal.css';

export default function SignOffModal({
    isOpen,
    onClose,
    selectedCheckpoint,
    signOffStatus,
    signOffNotes,
    setSignOffNotes,
    onConfirm
}) {
    if (!isOpen || !selectedCheckpoint) return null;

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    return (
        <div className="modal-overlay" onClick={handleOverlayClick}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <header className="modal-header">
                    <h3 className="modal-title">
                        {signOffStatus === 'pass' ? 'Sign Off' : 'Undo'} Checkpoint
                    </h3>
                    <button 
                        className="modal-close"
                        onClick={onClose}
                        aria-label="Close modal"
                    >
                        ×
                    </button>
                </header>
                
                <div className="modal-body">
                    <div className="checkpoint-summary">
                        <h4>{selectedCheckpoint.name}</h4>
                        <p>{selectedCheckpoint.description}</p>
                        <span className="checkpoint-points-badge">
                            {selectedCheckpoint.points} point
                        </span>
                    </div>
                    
                    <div className="notes-section">
                        <label htmlFor="signoff-notes" className="notes-label">
                            Notes
                        </label>
                        <textarea
                            id="signoff-notes"
                            className="notes-textarea"
                            placeholder={signOffStatus === 'pass' 
                                ? "Optional feedback or comments..." 
                                : "Optional notes for undoing this checkpoint..."
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
                        onClick={onClose}
                    >
                        Cancel
                    </button>
                    <button 
                        className={`modal-btn primary ${signOffStatus}`}
                        onClick={onConfirm}
                    >
                        {signOffStatus === 'pass' ? 'Confirm Sign Off' : 'Confirm Undo'}
                    </button>
                </footer>
            </div>
        </div>
    );
}