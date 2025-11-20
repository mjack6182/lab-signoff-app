import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './select-student.css';
import { api } from '../../config/api';

export default function SelectStudent() {
    const [selectedStudent, setSelectedStudent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [takenStudents, setTakenStudents] = useState(new Set());

    const navigate = useNavigate();
    const location = useLocation();

    // Get lab context and students from navigation state
    const {
        labCode,
        students,
        labId,
        labTitle,
        classId,
        className
    } = location.state || {};

    // Redirect back if no lab data
    if (!labCode || !students || !labId) {
        navigate('/lab-join');
        return null;
    }

    useEffect(() => {
        // Check for existing active session - if student already joined, redirect them
        if (students) {
            for (const student of students) {
                const sessionKey = `lab_session_${labCode}_${student}`;
                const sessionData = localStorage.getItem(sessionKey);
                if (sessionData) {
                    try {
                        const parsed = JSON.parse(sessionData);
                        // Check if session is less than 8 hours old
                        if (Date.now() - parsed.timestamp < 8 * 60 * 60 * 1000) {
                            // Auto-redirect to their existing session
                            navigate(`/student-checkpoints/${parsed.labId}/${parsed.groupId}`, {
                                state: parsed
                            });
                            return;
                        }
                    } catch (e) {
                        // Clean up corrupted session data
                        localStorage.removeItem(sessionKey);
                    }
                }
            }
        }

        // Check which students are already taken in this lab
        const takenKey = `taken_students_${labCode}`;
        const taken = JSON.parse(localStorage.getItem(takenKey) || '[]');
        setTakenStudents(new Set(taken));

        // Check if this browser already selected someone
        const mySelectionKey = `my_selection_${labCode}`;
        const mySelection = localStorage.getItem(mySelectionKey);
        if (mySelection && !taken.includes(mySelection)) {
            setSelectedStudent(mySelection);
        }
    }, [labCode, students, navigate]); const handleStudentSelect = (student) => {
        if (takenStudents.has(student)) {
            setError(`${student} is already selected in another tab. Please choose a different name.`);
            return;
        }

        // Clear any previous selection from localStorage
        const takenKey = `taken_students_${labCode}`;
        const mySelectionKey = `my_selection_${labCode}`;
        const previousSelection = localStorage.getItem(mySelectionKey);

        let taken = JSON.parse(localStorage.getItem(takenKey) || '[]');

        // Remove previous selection if it exists
        if (previousSelection) {
            taken = taken.filter(name => name !== previousSelection);
        }

        // Add new selection
        taken.push(student);
        localStorage.setItem(takenKey, JSON.stringify(taken));
        localStorage.setItem(mySelectionKey, student);

        setSelectedStudent(student);
        setTakenStudents(new Set(taken));
        setError(null);
    };

    const handleJoinSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!selectedStudent) {
            setError('Please select your name');
            return;
        }

        if (takenStudents.has(selectedStudent)) {
            setError(`${selectedStudent} is already selected in another tab. Please refresh and choose a different name.`);
            return;
        }

        setSubmitting(true);

        try {
            const response = await fetch(api.labJoinStudent(labCode), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    studentName: selectedStudent
                })
            });

            const joinData = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(joinData.error || 'Unable to join lab with that code');
            }

            const { lab: joinedLab, group } = joinData;
            if (!joinedLab || !group) {
                throw new Error('Unexpected response from server');
            }

            // Store session data for recovery if tab is closed
            const sessionData = {
                studentName: selectedStudent,
                labCode: joinedLab.labCode,
                labTitle: joinedLab.labTitle,
                classId: joinedLab.classId,
                className: joinedLab.className,
                groupId: group.id,
                groupDisplayId: group.groupId,
                labId: joinedLab.labId,
                labData: joinedLab,
                labCheckpoints: joinedLab.checkpoints,
                groupData: group,
                timestamp: Date.now()
            };
            localStorage.setItem(`lab_session_${joinedLab.labCode}_${selectedStudent}`, JSON.stringify(sessionData));

            navigate(`/student-checkpoints/${joinedLab.labId}/${group.id}`, {
                state: sessionData
            });
        } catch (err) {
            setError(err.message || 'Failed to join lab');
        } finally {
            setSubmitting(false);
        }
    };

    const handleBackToCode = () => {
        navigate('/lab-join');
    };

    const handleRecoverSession = () => {
        // Look for any active session for this lab
        if (students) {
            for (const student of students) {
                const sessionKey = `lab_session_${labCode}_${student}`;
                const sessionData = localStorage.getItem(sessionKey);
                if (sessionData) {
                    try {
                        const parsed = JSON.parse(sessionData);
                        // Check if session is less than 8 hours old
                        if (Date.now() - parsed.timestamp < 8 * 60 * 60 * 1000) {
                            navigate(`/student-checkpoints/${parsed.labId}/${parsed.groupId}`, {
                                state: parsed
                            });
                            return;
                        } else {
                            // Clean up expired session
                            localStorage.removeItem(sessionKey);
                        }
                    } catch (e) {
                        localStorage.removeItem(sessionKey);
                    }
                }
            }
        }
        setError('No recent lab session found. Please select your name to join the lab.');
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

                {/* Recovery option for students who closed their tab */}
                <div style={{ marginBottom: '20px', textAlign: 'center' }}>
                    <button
                        type="button"
                        onClick={handleRecoverSession}
                        className="change-code-button"
                        style={{
                            background: '#f0fdf4',
                            border: '1px solid #22c55e',
                            color: '#15803d',
                            padding: '8px 16px',
                            fontSize: '14px'
                        }}
                    >
                        Return to My Lab Session
                    </button>
                    <p style={{ fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' }}>
                        Click if you accidentally closed your lab tab
                    </p>
                </div>

                <form onSubmit={handleJoinSubmit} className="select-student-form">
                    <div className="form-group">
                        <label className="form-label">Your Name</label>
                        <div className="student-blocks-grid">
                            {students.map((student, index) => (
                                <button
                                    key={index}
                                    type="button"
                                    className={`student-block ${selectedStudent === student ? 'selected' : ''
                                        } ${takenStudents.has(student) && selectedStudent !== student ? 'blocked' : ''}`}
                                    onClick={() => handleStudentSelect(student)}
                                    disabled={submitting || (takenStudents.has(student) && selectedStudent !== student)}
                                >
                                    {student}
                                    {takenStudents.has(student) && selectedStudent !== student && (
                                        <span className="selected-indicator">✓</span>
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="join-button"
                        disabled={!selectedStudent || submitting}
                    >
                        {submitting ? 'Joining Lab...' : 'Join Lab'}
                    </button>
                </form>
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
