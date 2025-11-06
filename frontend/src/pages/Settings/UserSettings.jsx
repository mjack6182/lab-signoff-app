import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import DeleteAccountModal from '../../components/DeleteAccountModal/DeleteAccountModal';
import './Settings.css';

export default function UserSettings() {
    const { user, updateUserProfile } = useAuth();
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    // Initialize form with current user data
    useEffect(() => {
        if (user) {
            setFirstName(user.firstName || '');
            setLastName(user.lastName || '');
        }
    }, [user]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess(false);

        // Validate input
        if (!firstName.trim() || !lastName.trim()) {
            setError('Please enter both first and last name');
            return;
        }

        setLoading(true);

        try {
            await updateUserProfile(firstName.trim(), lastName.trim());
            setSuccess(true);
            setTimeout(() => setSuccess(false), 3000);
        } catch (err) {
            setError(err.message || 'Failed to update profile. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="settings-card">
                <div className="card-section">
                    <h2 className="section-title">Profile Information</h2>
                    <p className="section-description">Update your personal details</p>
                </div>

                <form onSubmit={handleSubmit} className="settings-form">
                    <div className="form-row">
                        <div className="form-field">
                            <label htmlFor="email" className="form-label">
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                className="form-input"
                                value={user?.email || ''}
                                disabled
                            />
                            <p className="form-hint">Email cannot be changed</p>
                        </div>
                    </div>

                    <div className="form-row">
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
                    </div>

                    {error && (
                        <div className="alert alert-error">
                            {error}
                        </div>
                    )}

                    {success && (
                        <div className="alert alert-success">
                            Profile updated successfully!
                        </div>
                    )}

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading}
                        >
                            {loading ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </form>
            </div>

            <div className="settings-card">
                <div className="card-section">
                    <h2 className="section-title">Account Information</h2>
                    <p className="section-description">View your account details</p>
                </div>

                <div className="info-grid">
                    <div className="info-item">
                        <span className="info-label">Role</span>
                        <span className="info-value">{user?.role || 'N/A'}</span>
                    </div>
                    <div className="info-item">
                        <span className="info-label">User ID</span>
                        <span className="info-value info-mono">{user?.mongoId || 'N/A'}</span>
                    </div>
                </div>
            </div>

            <div className="settings-card danger-card">
                <div className="card-section">
                    <h2 className="section-title section-title-danger">Danger Zone</h2>
                    <p className="section-description">Irreversible account actions</p>
                </div>

                <div className="danger-zone">
                    <div className="danger-item">
                        <div className="danger-item-info">
                            <h3 className="danger-item-title">Delete Account</h3>
                            <p className="danger-item-description">
                                Permanently delete your account and all associated data. This action cannot be undone.
                            </p>
                        </div>
                        <button
                            type="button"
                            className="btn btn-danger"
                            onClick={() => setShowDeleteModal(true)}
                        >
                            Delete Account
                        </button>
                    </div>
                </div>
            </div>

            <DeleteAccountModal
                isOpen={showDeleteModal}
                onClose={() => setShowDeleteModal(false)}
            />
        </>
    );
}
