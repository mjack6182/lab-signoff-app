import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockAuth = {
  user: null,
  logout: vi.fn(),
  isAuthenticated: false,
};

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

import Header from '../components/Header/Header';

describe('Header', () => {
  beforeEach(() => {
    Object.assign(mockAuth, {
      user: {
        firstName: 'Alex',
        lastName: 'Teacher',
        email: 'alex@test.com',
        role: 'Teacher',
      },
      isAuthenticated: true,
    });
    mockNavigate.mockReset();
    mockAuth.logout.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  const openMenu = () => {
    fireEvent.click(screen.getByLabelText('User menu'));
  };

  it('displays user info and navigates to settings', () => {
    render(<Header />);

    expect(screen.getByText('Alex Teacher')).toBeInTheDocument();
    expect(screen.getByText('(Teacher)')).toBeInTheDocument();

    openMenu();
    fireEvent.click(screen.getByText('Settings'));

    expect(mockNavigate).toHaveBeenCalledWith('/settings');
  });

  it('logs out and redirects to login', () => {
    render(<Header />);

    openMenu();
    fireEvent.click(screen.getByText('Logout'));

    expect(mockAuth.logout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
});
