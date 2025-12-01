import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import './ProfileCompletionModal.css';

export default function ProfileCompletionModal({ isOpen }) {
    const { updateUserProfile } = useAuth();
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validate input
        if (!firstName.trim() || !lastName.trim()) {
            setError('Please enter both first and last name');
            return;
        }

        setError('');

        setLoading(true);

        try {
            await updateUserProfile(firstName.trim(), lastName.trim());
            // Modal will close automatically when hasCompletedProfile becomes true
        } catch (err) {
            setError(err.message || 'Failed to update profile. Please try again.');
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <header className="modal-header">
                    <h3 className="modal-title">Complete Your Profile</h3>
                </header>

                <div className="modal-body">
                    <div className="profile-intro">
                        <p>Welcome! Please provide your name to complete your profile.</p>
                    </div>

                    <form onSubmit={handleSubmit} className="profile-form" noValidate>
                        <div className="form-field">
                            <label htmlFor="firstName" className="form-label">
                                First Name <span className="required">*</span>
                            </label>
                            <input
                                id="firstName"
                                type="text"
                                className="form-input"
                                placeholder="Enter your first name"
                                value={firstName}
                                onChange={(e) => setFirstName(e.target.value)}
                                disabled={loading}
                                autoFocus
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="lastName" className="form-label">
                                Last Name <span className="required">*</span>
                            </label>
                            <input
                                id="lastName"
                                type="text"
                                className="form-input"
                                placeholder="Enter your last name"
                                value={lastName}
                                onChange={(e) => setLastName(e.target.value)}
                                disabled={loading}
                                required
                            />
                        </div>

                        {error && (
                            <div className="error-message">
                                {error}
                            </div>
                        )}

                        <footer className="modal-footer">
                            <button
                                type="submit"
                                className="modal-btn primary"
                                disabled={loading}
                            >
                                {loading ? 'Saving...' : 'Complete Profile'}
                            </button>
                        </footer>
                    </form>
                </div>
            </div>
        </div>
    );
}
