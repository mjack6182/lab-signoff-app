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
            'Alice Johnson', 'Bob Smith', 'Charlie Brown', 'Diana Wilson', 'Ethan Davis',
            'Fiona Clark', 'George Miller', 'Hannah Lee', 'Ian Taylor', 'Julia Anderson',
            'Kevin Chen', 'Laura Martinez', 'Michael Thompson', 'Nina Patel', 'Oscar Rodriguez',
            'Priya Singh', 'Quinn Walker', 'Rachel Green', 'Sam Williams', 'Tina Park',
            'Uma Sharma', 'Victor Lopez', 'Wendy Chang', 'Xavier Kim', 'Yuki Tanaka',
            'Zoe Adams', 'Aaron Brooks', 'Bella Carter', 'Caleb Davis', 'Daisy Evans'
        ],
        'CS201': [
            'Alex Thompson', 'Brooke Wilson', 'Connor Mitchell', 'Delia Rodriguez', 'Eli Foster',
            'Grace Patel', 'Harrison Lee', 'Ivy Chen', 'Jake Morrison', 'Kara Johnson',
            'Liam Anderson', 'Maya Singh', 'Noah Kim', 'Olivia Brown', 'Parker Davis',
            'Quinn Taylor', 'Ruby Martinez', 'Seth Wilson', 'Tara Nguyen', 'Umar Ali',
            'Violet Clark', 'Wesley Park', 'Ximena Lopez', 'Yasmin Ahmed', 'Zach Miller',
            'Aria Shah', 'Blake Cooper', 'Chloe Wright', 'Diego Ramirez', 'Emma Stone'
        ],
        'MATH150': [
            'Aiden Murphy', 'Bella Torres', 'Cameron White', 'Delilah Garcia', 'Evan Phillips',
            'Faith Robinson', 'Gabriel Hayes', 'Harper Collins', 'Isaac Reed', 'Jasmine Cook',
            'Kai Edwards', 'Luna Rivera', 'Mason Hughes', 'Nora Stewart', 'Owen Bailey',
            'Penelope Ward', 'Quinton Gray', 'Riley Murphy', 'Sophia Barnes', 'Tyler Nelson',
            'Ursula Powell', 'Vincent Scott', 'Willow Fisher', 'Xander King', 'Yvonne Bell',
            'Zachary Price', 'Abigail Ross', 'Benjamin Cruz', 'Cora Morgan', 'Dante Kelly'
        ],
        'default': [
            'Student Adams', 'Student Baker', 'Student Clark', 'Student Davis', 'Student Evans',
            'Student Foster', 'Student Garcia', 'Student Harris', 'Student Irving', 'Student Jones',
            'Student Kelly', 'Student Lopez', 'Student Miller', 'Student Nelson', 'Student Oliver',
            'Student Parker', 'Student Quinn', 'Student Rivera', 'Student Smith', 'Student Taylor',
            'Student Underwood', 'Student Valdez', 'Student Wilson', 'Student Xavier', 'Student Young',
            'Student Zhang', 'Student Anderson', 'Student Brown', 'Student Carter', 'Student Douglas'
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
            </div>            <div className="app-info">
                <h2 className="app-info-title">Lab Sign Off</h2>
                <p className="app-info-description">
                    Track your lab progress and complete checkpoints
                </p>
            </div>
        </main>
    );
}