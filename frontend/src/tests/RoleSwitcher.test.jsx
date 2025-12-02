import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';

const mockAuth = {
  user: null,
  switchRole: vi.fn(),
};

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

import RoleSwitcher from '../components/RoleSwitcher/RoleSwitcher';

describe('RoleSwitcher', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('returns null when no user', () => {
    const { container } = render(<RoleSwitcher />);
    expect(container.firstChild).toBeNull();
  });

  it('renders role buttons and switches roles', () => {
    Object.assign(mockAuth, {
      user: { role: 'Teacher' },
      switchRole: vi.fn(),
    });

    render(<RoleSwitcher />);

    fireEvent.click(screen.getByRole('button', { name: 'Teaching Assistant' }));
    expect(mockAuth.switchRole).toHaveBeenCalledWith('TA');

    const currentRole = screen.getByText('Teacher', { selector: '.role-switcher-current' });
    expect(currentRole).toBeInTheDocument();
  });
});
