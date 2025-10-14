import { TeacherOnly, TAOnly, StaffOnly, StudentOnly, useRoleGuard } from '../../components/RoleGuard/RoleGuard';
import { useAuth } from '../../contexts/AuthContext';

export default function RoleDemo() {
    const { user } = useAuth();
    const { isTeacher, isTA, isStudent, isTeacherOrTA } = useRoleGuard();

    return (
        <main className="role-demo-page">
            <header className="role-demo-header">
                <h1>Role-Based Access Control Demo</h1>
                <p>This page demonstrates how different UI elements are shown/hidden based on user roles.</p>
                {user && (
                    <div className="current-user-info">
                        <strong>Current User:</strong> {user.name} ({user.role})
                    </div>
                )}
            </header>

            <div className="role-demo-grid">
                {/* Teacher Only Section */}
                <div className="demo-section">
                    <h2>Teacher Only Features</h2>
                    <TeacherOnly 
                        fallback={<div className="access-denied">Access Denied: Teacher role required</div>}
                        showFallback={true}
                    >
                        <div className="feature-card teacher">
                            <h3>✅ Teacher Dashboard</h3>
                            <ul>
                                <li>Create new lab assignments</li>
                                <li>Manage course settings</li>
                                <li>View analytics and reports</li>
                                <li>Export grade data</li>
                            </ul>
                        </div>
                    </TeacherOnly>
                </div>

                {/* TA Only Section */}
                <div className="demo-section">
                    <h2>TA Only Features</h2>
                    <TAOnly 
                        fallback={<div className="access-denied">Access Denied: TA role required</div>}
                        showFallback={true}
                    >
                        <div className="feature-card ta">
                            <h3>✅ TA Assistant Tools</h3>
                            <ul>
                                <li>Bulk grading tools</li>
                                <li>Student progress monitoring</li>
                                <li>Office hours scheduling</li>
                                <li>Assignment reminders</li>
                            </ul>
                        </div>
                    </TAOnly>
                </div>

                {/* Staff Only Section */}
                <div className="demo-section">
                    <h2>Staff Features (Teacher or TA)</h2>
                    <StaffOnly 
                        fallback={<div className="access-denied">Access Denied: Staff role required</div>}
                        showFallback={true}
                    >
                        <div className="feature-card staff">
                            <h3>✅ Staff Tools</h3>
                            <ul>
                                <li>Grade checkpoints</li>
                                <li>View all student submissions</li>
                                <li>Send feedback</li>
                                <li>Manage group assignments</li>
                            </ul>
                        </div>
                    </StaffOnly>
                </div>

                {/* Student Only Section */}
                <div className="demo-section">
                    <h2>Student Features</h2>
                    <StudentOnly 
                        fallback={<div className="access-denied">Access Denied: Student role required</div>}
                        showFallback={true}
                    >
                        <div className="feature-card student">
                            <h3>✅ Student Portal</h3>
                            <ul>
                                <li>View assignment progress</li>
                                <li>Submit checkpoint work</li>
                                <li>Check grades and feedback</li>
                                <li>Join study groups</li>
                            </ul>
                        </div>
                    </StudentOnly>
                </div>

                {/* Conditional Features */}
                <div className="demo-section full-width">
                    <h2>Conditional Features</h2>
                    <div className="conditional-grid">
                        {isTeacher && (
                            <div className="feature-card conditional">
                                <h4>🎓 Teacher Dashboard</h4>
                                <p>Advanced analytics and course management</p>
                            </div>
                        )}
                        
                        {isTA && (
                            <div className="feature-card conditional">
                                <h4>🤝 TA Tools</h4>
                                <p>Student assistance and grading support</p>
                            </div>
                        )}
                        
                        {isTeacherOrTA && (
                            <div className="feature-card conditional">
                                <h4>⚡ Quick Actions</h4>
                                <p>Fast checkpoint approval and feedback</p>
                            </div>
                        )}
                        
                        {isStudent && (
                            <div className="feature-card conditional">
                                <h4>📚 My Progress</h4>
                                <p>Track your learning journey</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </main>
    );
}