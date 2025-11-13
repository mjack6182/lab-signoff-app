import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../../services/apiService.js';
import './lab-join.css';

export default function LabJoin() {
    const [labCode, setLabCode] = useState('');
    const [loadingStudents, setLoadingStudents] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    const navigate = useNavigate();

    const handleCodeSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (!labCode.trim()) {
            setError('Please enter a lab code');
            return;
        }

        setLoadingStudents(true);

        try {
            // Fetch lab data using the join code
            const labData = await apiService.joinLabWithCode(labCode.trim());

            if (!labData) {
                throw new Error('No lab found with this code');
            }

            // Get the roster for this lab
            const roster = await apiService.getLabRoster(labData.id);

            if (!roster || !roster.students || roster.students.length === 0) {
                throw new Error('No students found in this lab roster');
            }

            setLoadingStudents(false);
            setSuccess(`Successfully found lab: ${labData.name || labCode}`);

            // Navigate to select student page with real data
            navigate('/select-student', {
                state: {
                    labCode: labCode.toUpperCase(),
                    labId: labData.id,
                    labName: labData.name,
                    students: roster.students,
                    labData: labData
                }
            });

        } catch (error) {
            setLoadingStudents(false);
            console.error('Error joining lab:', error);

            // Handle specific error cases
            if (error.message.includes('404') || error.message.includes('not found')) {
                setError(`Lab code "${labCode.toUpperCase()}" not found. Please check the code and try again.`);
            } else if (error.message.includes('403') || error.message.includes('unauthorized')) {
                setError('You are not authorized to join this lab. Please contact your instructor.');
            } else if (error.message.includes('network') || error.message.includes('fetch')) {
                setError('Network error. Please check your connection and try again.');
            } else {
                setError(error.message || 'Failed to join lab. Please try again.');
            }
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
                {success && <div className="success-message">{success}</div>}

                <form onSubmit={handleCodeSubmit} className="lab-join-form">
                    <div className="form-group">
                        <label htmlFor="labCode" className="form-label">Lab Code</label>
                        <input
                            id="labCode"
                            type="text"
                            className="form-input"
                            value={labCode}
                            onChange={(e) => {
                                setLabCode(e.target.value);
                                setError(null);
                                setSuccess(null);
                            }}
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
            </div>            <div className="app-info">
                <h2 className="app-info-title">Lab Sign Off</h2>
                <p className="app-info-description">
                    Track your lab progress and complete checkpoints
                </p>
            </div>
        </main>
    );
}