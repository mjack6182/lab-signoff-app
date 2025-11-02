import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import './DeleteAccountModal.css';

export default function DeleteAccountModal({ isOpen, onClose }) {
    const { deleteAccount } = useAuth();
    const [confirmText, setConfirmText] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const REQUIRED_TEXT = 'delete this account';
    const isConfirmValid = confirmText.toLowerCase() === REQUIRED_TEXT;

    const handleDelete = async () => {
        if (!isConfirmValid) {
            setError('Please type the exact confirmation text');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await deleteAccount();
            // User will be logged out automatically after successful deletion
        } catch (err) {
            setError(err.message || 'Failed to delete account. Please try again.');
            setLoading(false);
        }
    };

    const handleClose = () => {
        if (!loading) {
            setConfirmText('');
            setError('');
            onClose();
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content delete-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-icon-danger">
                        <i className="fas fa-exclamation-triangle"></i>
                    </div>
                    <h2 className="modal-title">Delete Account</h2>
                </div>

                <div className="modal-body">
                    <p className="modal-description">
                        This action cannot be undone. This will permanently delete your account and remove all your data from our servers.
                    </p>

                    <div className="warning-box">
                        <h3 className="warning-title">Before you proceed:</h3>
                        <ul className="warning-list">
                            <li>All your personal information will be permanently deleted</li>
                            <li>You will lose access to all your classes and checkpoints</li>
                            <li>This action is irreversible and cannot be undone</li>
                        </ul>
                    </div>

                    <div className="confirmation-section">
                        <label htmlFor="confirmText" className="confirmation-label">
                            To confirm, type <strong>&quot;{REQUIRED_TEXT}&quot;</strong> below:
                        </label>
                        <input
                            id="confirmText"
                            type="text"
                            className="confirmation-input"
                            placeholder="Type here..."
                            value={confirmText}
                            onChange={(e) => setConfirmText(e.target.value)}
                            disabled={loading}
                            autoComplete="off"
                        />
                    </div>

                    {error && (
                        <div className="alert alert-error">
                            {error}
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={handleClose}
                        disabled={loading}
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        className="btn btn-danger"
                        onClick={handleDelete}
                        disabled={!isConfirmValid || loading}
                    >
                        {loading ? 'Deleting Account...' : 'Delete Account'}
                    </button>
                </div>
            </div>
        </div>
    );
}
