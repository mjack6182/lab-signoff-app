import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './lab-join.css';
import { api } from '../../config/api';

export default function LabJoin() {
    const [labCode, setLabCode] = useState('');
    const [loadingStudents, setLoadingStudents] = useState(false);
    const [error, setError] = useState(null);

    const navigate = useNavigate();

    const handleCodeSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        const trimmedCode = labCode.trim().toUpperCase();
        if (!trimmedCode) {
            setError('Please enter a lab code');
            return;
        }

        setLoadingStudents(true);

        try {
            const response = await fetch(api.labByJoinCode(trimmedCode));
            const labData = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(labData.error || 'Unable to find a lab with that code');
            }

            const students = Array.isArray(labData.students) ? labData.students : [];
            if (students.length === 0) {
                throw new Error('No students found for this lab code');
            }

            navigate('/select-student', {
                state: {
                    labCode: labData.labCode || trimmedCode,
                    students,
                    labId: labData.labId,
                    labTitle: labData.labTitle,
                    classId: labData.classId,
                    className: labData.className
                }
            });
        } catch (err) {
            setError(err.message || 'Failed to load class roster');
        } finally {
            setLoadingStudents(false);
        }
    };

    return (
        <main className="lab-join-container">
            <div className="lab-join-card">
                <div className="lab-join-header">
                    <h1 className="lab-join-title">Join Lab</h1>
                    <p className="lab-join-subtitle">Enter your lab code to continue</p>
                </div>

                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleCodeSubmit} className="lab-join-form">
                    <div className="form-group">
                        <label htmlFor="labCode" className="form-label">Lab Code</label>
                        <input
                            id="labCode"
                            type="text"
                            className="form-input"
                            value={labCode}
                            onChange={(e) => setLabCode(e.target.value)}
                            placeholder="Enter lab code (e.g., CS101, CS201, MATH150)"
                            disabled={loadingStudents}
                        />
                    </div>

                    <button
                        type="submit"
                        className="join-button"
                        disabled={loadingStudents}
                    >
                        {loadingStudents ? 'Loading Class Roster...' : 'Continue'}
                    </button>
                </form>
            </div>
            <div className="app-info">
                <h2 className="app-info-title">Lab Sign Off</h2>
                <p className="app-info-description">
                    Track your lab progress and complete checkpoints
                </p>
            </div>
        </main>
    );
}
