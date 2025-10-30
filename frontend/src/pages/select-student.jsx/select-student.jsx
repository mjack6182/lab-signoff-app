import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './select-student.css';

export default function SelectStudent() {
    const [selectedStudent, setSelectedStudent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    const navigate = useNavigate();
    const location = useLocation();

    // Get lab code and students from navigation state
    const { labCode, students } = location.state || {};

    // Redirect back if no lab data
    if (!labCode || !students) {
        navigate('/lab-join');
        return null;
    }

    const handleJoinSubmit = (e) => {
        e.preventDefault();
        setError(null);

        if (!selectedStudent) {
            setError('Please select your name');
            return;
        }

        setSubmitting(true);

        // Simulate joining lab
        setTimeout(() => {
            setSubmitting(false);
            console.log('Joining lab:', { labCode, selectedStudent });
            // Navigate to next page (you'll tell me about this)
            // navigate('/lab-dashboard');
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

                <form onSubmit={handleJoinSubmit} className="select-student-form">
                    <div className="form-group">
                        <label htmlFor="studentName" className="form-label">Your Name</label>
                        <select
                            id="studentName"
                            className="form-select"
                            value={selectedStudent}
                            onChange={(e) => setSelectedStudent(e.target.value)}
                            disabled={submitting}
                        >
                            <option value="">Choose your name...</option>
                            {students.map((student, index) => (
                                <option key={index} value={student}>
                                    {student}
                                </option>
                            ))}
                        </select>
                    </div>

                    <button
                        type="submit"
                        className="join-button"
                        disabled={submitting}
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