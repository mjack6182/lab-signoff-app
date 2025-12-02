import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';

const mockAuth = { deleteAccount: vi.fn() };

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

import DeleteAccountModal from '../components/DeleteAccountModal/DeleteAccountModal';

describe('DeleteAccountModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('does not render when closed', () => {
    render(<DeleteAccountModal isOpen={false} onClose={vi.fn()} />);
    expect(screen.queryByText('Delete Account')).not.toBeInTheDocument();
  });

  it('requires confirmation text before enabling delete', () => {
    render(<DeleteAccountModal isOpen onClose={vi.fn()} />);

    const deleteButton = screen.getByRole('button', { name: 'Delete Account' });
    expect(deleteButton).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/type "delete this account"/i), {
      target: { value: 'DELETE THIS ACCOUNT' },
    });

    expect(deleteButton).toBeEnabled();
  });

  it('calls deleteAccount when confirmation matches', async () => {
    render(<DeleteAccountModal isOpen onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/type "delete this account"/i), {
      target: { value: 'delete this account' },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Delete Account' }));

    await waitFor(() => {
      expect(mockAuth.deleteAccount).toHaveBeenCalledTimes(1);
    });
  });

  it('resets fields and calls onClose when cancelling', () => {
    const onClose = vi.fn();
    render(<DeleteAccountModal isOpen onClose={onClose} />);

    const input = screen.getByLabelText(/type "delete this account"/i);
    fireEvent.change(input, { target: { value: 'delete this account' } });

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalled();
    expect(input).toHaveValue('');
  });
});
