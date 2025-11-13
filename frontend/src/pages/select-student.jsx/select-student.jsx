import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './select-student.css';

export default function SelectStudent() {
    const [selectedStudent, setSelectedStudent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [alreadySelected, setAlreadySelected] = useState(null);

    const navigate = useNavigate();
    const location = useLocation();

    // Generate a simple browser fingerprint
    const generateFingerprint = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        ctx.textBaseline = 'top';
        ctx.font = '14px Arial';
        ctx.fillText('Browser fingerprint', 2, 2);

        const fingerprint = [
            navigator.userAgent,
            navigator.language,
            screen.width + 'x' + screen.height,
            new Date().getTimezoneOffset(),
            canvas.toDataURL()
        ].join('|');

        // Simple hash function
        let hash = 0;
        for (let i = 0; i < fingerprint.length; i++) {
            const char = fingerprint.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32-bit integer
        }
        return Math.abs(hash).toString(36);
    };

    // Check if this browser has already selected a student for this lab
    const checkExistingSelection = () => {
        const fingerprint = generateFingerprint();
        const selectionKey = `lab_selection_${labCode}_${fingerprint}`;
        const existingSelection = localStorage.getItem(selectionKey);

        if (existingSelection) {
            const selection = JSON.parse(existingSelection);
            const selectionTime = new Date(selection.timestamp);
            const now = new Date();
            const hoursSinceSelection = (now - selectionTime) / (1000 * 60 * 60);

            // Allow re-selection after 24 hours (in case of legitimate need)
            if (hoursSinceSelection < 24) {
                return selection.studentName;
            } else {
                // Clean up old selection
                localStorage.removeItem(selectionKey);
            }
        }
        return null;
    };

    // Record the selection
    const recordSelection = (studentName) => {
        const fingerprint = generateFingerprint();
        const selectionKey = `lab_selection_${labCode}_${fingerprint}`;
        const selection = {
            studentName,
            labCode,
            timestamp: new Date().toISOString(),
            fingerprint
        };
        localStorage.setItem(selectionKey, JSON.stringify(selection));
    };

    // Get lab code and students from navigation state
    const { labCode, students } = location.state || {};

    // Redirect back if no lab data
    if (!labCode || !students) {
        navigate('/lab-join');
        return null;
    }

    // Check for existing selection on component mount
    useEffect(() => {
        const existingSelection = checkExistingSelection();
        if (existingSelection) {
            setAlreadySelected(existingSelection);
            setError(`This browser has already selected "${existingSelection}" for ${labCode}. Please use a different device or wait 24 hours to select again.`);
        }
    }, [labCode]);

    const handleJoinSubmit = () => {
        setError(null);

        if (!selectedStudent) {
            setError('Please select your name');
            return;
        }

        // Check if this browser already has a selection
        const existingSelection = checkExistingSelection();
        if (existingSelection) {
            setError(`This browser has already selected "${existingSelection}" for ${labCode}. Cannot select multiple students.`);
            return;
        }

        setSubmitting(true);

        // Simulate joining lab
        setTimeout(() => {
            // Record the selection before proceeding
            recordSelection(selectedStudent);

            setSubmitting(false);
            console.log('Joining lab:', { labCode, selectedStudent });

            // Create a mock group ID based on lab code and student name
            // In a real app, this would come from the backend after joining
            const mockGroupId = `${labCode}-group-${Math.floor(Math.random() * 10) + 1}`;
            const mockLabId = labCode.toLowerCase();

            // Navigate to student checkpoints page
            navigate(`/student-checkpoints/${mockLabId}/${mockGroupId}`, {
                state: {
                    studentName: selectedStudent,
                    labCode: labCode,
                    groupId: mockGroupId
                }
            });
        }, 1000);
    };

    const handleBackToCode = () => {
        navigate('/lab-join');
    };

    return (
        <main className="select-student-container">
            <div className="select-student-card">
                <div className="select-student-header">
                    <h1 className="select-student-title">Select Your Name</h1>
                    <p className="select-student-subtitle">Choose your name from the class roster</p>
                </div>

                <div className="lab-code-display">
                    <span className="lab-code-label">Lab Code:</span>
                    <span className="lab-code-value">{labCode}</span>
                    <button
                        type="button"
                        className="change-code-button"
                        onClick={handleBackToCode}
                    >
                        Change
                    </button>
                </div>

                {error && <div className="error-message">{error}</div>}

                <div className="select-student-form">
                    <div className="form-group">
                        <label className="form-label">Your Name</label>
                        <div className="student-blocks-grid">
                            {students.map((student, index) => (
                                <button
                                    key={index}
                                    type="button"
                                    className={`student-block ${selectedStudent === student ? 'selected' : ''} ${alreadySelected ? 'blocked' : ''}`}
                                    onClick={() => !alreadySelected && setSelectedStudent(student)}
                                    disabled={submitting || alreadySelected}
                                >
                                    {student}
                                    {alreadySelected === student && <span className="selected-indicator"> ✓ Selected</span>}
                                </button>
                            ))}
                        </div>
                    </div>

                    <button
                        type="button"
                        className="join-button"
                        disabled={submitting || !selectedStudent || alreadySelected}
                        onClick={handleJoinSubmit}
                    >
                        {alreadySelected ? 'Student Already Selected' :
                            submitting ? 'Joining Lab...' : 'Join Lab'}
                    </button>
                </div>
            </div>

            <div className="app-info">
                <h2 className="app-info-title">LabTracker</h2>
                <p className="app-info-description">
                    Track your lab progress and complete checkpoints
                </p>
            </div>
        </main>
    );
}