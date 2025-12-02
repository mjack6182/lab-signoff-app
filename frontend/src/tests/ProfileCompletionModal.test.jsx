import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';

const mockAuth = { updateUserProfile: vi.fn() };

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

import ProfileCompletionModal from '../components/ProfileCompletionModal/ProfileCompletionModal';

describe('ProfileCompletionModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('does not render when closed', () => {
    render(<ProfileCompletionModal isOpen={false} />);
    expect(screen.queryByText('Complete Your Profile')).not.toBeInTheDocument();
  });

  it('validates required fields', () => {
    render(<ProfileCompletionModal isOpen />);

    fireEvent.click(screen.getByRole('button', { name: 'Complete Profile' }));
    expect(screen.getByText('Please enter both first and last name')).toBeInTheDocument();
  });

  it('submits trimmed names', async () => {
    mockAuth.updateUserProfile.mockResolvedValue({});
    render(<ProfileCompletionModal isOpen />);

    fireEvent.change(screen.getByLabelText('First Name *'), { target: { value: '  Alex ' } });
    fireEvent.change(screen.getByLabelText('Last Name *'), { target: { value: ' Teacher ' } });

    fireEvent.click(screen.getByRole('button', { name: 'Complete Profile' }));

    await waitFor(() =>
      expect(mockAuth.updateUserProfile).toHaveBeenCalledWith('Alex', 'Teacher'),
    );
  });
});
