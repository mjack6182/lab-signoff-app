import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './select-student.css';
import { api } from '../../config/api';

export default function SelectStudent() {
    const [selectedStudent, setSelectedStudent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

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

    const handleStudentSelect = (student) => {
        setSelectedStudent(student);
        setError(null);
    };

    const handleJoinSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!selectedStudent) {
            setError('Please select your name');
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

                <form onSubmit={handleJoinSubmit} className="select-student-form">
                    <div className="form-group">
                        <label className="form-label">Your Name</label>
                        <div className="student-blocks-grid">
                            {students.map((student, index) => (
                                <button
                                    key={index}
                                    type="button"
                                    className={`student-block ${selectedStudent === student ? 'selected' : ''}`}
                                    onClick={() => handleStudentSelect(student)}
                                    disabled={submitting}
                                >
                                    {student}
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
