import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './lab-join.css';

export default function LabJoin() {
    const [labCode, setLabCode] = useState('');
    const [loadingStudents, setLoadingStudents] = useState(false);
    const [error, setError] = useState(null);
    
    const navigate = useNavigate();    // Mock student lists for different lab codes
    const mockStudentLists = {
        'CS101': [
            'Alice Johnson',
            'Bob Smith',
            'Charlie Brown',
            'Diana Wilson',
            'Ethan Davis'
        ],
        'CS201': [
            'Fiona Clark',
            'George Miller',
            'Hannah Lee',
            'Ian Taylor',
            'Julia Anderson'
        ],
        'MATH150': [
            'Kevin Chen',
            'Laura Martinez',
            'Michael Thompson',
            'Nina Patel',
            'Oscar Rodriguez'
        ],
        'default': [
            'Student One',
            'Student Two',
            'Student Three',
            'Student Four',
            'Student Five'
        ]
    };

    const handleCodeSubmit = (e) => {
        e.preventDefault();
        setError(null);

        if (!labCode.trim()) {
            setError('Please enter a lab code');
            return;
        }

        setLoadingStudents(true);

        // Simulate API call to fetch students for this lab code
        setTimeout(() => {
            const studentList = mockStudentLists[labCode.toUpperCase()] || mockStudentLists['default'];
            setLoadingStudents(false);
            // Navigate to select student page with lab data
            navigate('/select-student', { 
                state: { 
                    labCode: labCode.toUpperCase(), 
                    students: studentList 
                } 
            });
        }, 800);
    };

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
        setStep(1);
        setSelectedStudent('');
        setStudents([]);
        setError(null);
    };

    return (
        <main className="lab-join-container">
            <div className="lab-join-card">
                <div className="lab-join-header">
                    <h1 className="lab-join-title">Join Lab</h1>
                    {step === 1 ? (
                        <p className="lab-join-subtitle">Enter your lab code to continue</p>
                    ) : (
                        <p className="lab-join-subtitle">Select your name from the class roster</p>
                    )}
                </div>

                {error && <div className="error-message">{error}</div>}

                {step === 1 ? (
                    // Step 1: Enter Lab Code
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
                ) : (
                    // Step 2: Select Student Name
                    <form onSubmit={handleJoinSubmit} className="lab-join-form">
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

                        <div className="form-group">
                            <label htmlFor="studentName" className="form-label">Select Your Name</label>
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
                )}
            </div>            <div className="app-info">
                <h2 className="app-info-title">Lab Sign Off</h2>
                <p className="app-info-description">
                    Track your lab progress and complete checkpoints
                </p>
            </div>
        </main>
    );
}