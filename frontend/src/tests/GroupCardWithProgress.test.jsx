import { vi } from 'vitest';

vi.mock('../../contexts/AuthContext', () => {
  return {
    useAuth: () => ({
      user: {
        id: 'test-user-id',
        role: 'Teacher',
        firstName: 'Test',
        lastName: 'User',
      },
      isTeacher: () => true,
      isTA: () => false,
      isStudent: () => false,
      isAdmin: () => false,
      isTeacherOrTA: () => true,
      isStaffOrAdmin: () => true,
      hasCompletedProfile: () => true,
      switchRole: vi.fn(),
    }),
  };
});

vi.mock('../components/RoleGuard/RoleGuard', () => ({
  StaffOnly: ({ children }) => <>{children}</>,
}));

import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';

import GroupCard from '../components/GroupCard/GroupCard';

const checkpoints = [
  { id: 'c1', name: 'Prep', completed: true },
  { id: 'c2', name: 'Build', completed: false },
];

const members = [
  { id: 'm1', name: 'Alex Teacher', role: 'Teacher' },
  { id: 'm2', name: 'Taylor TA', role: 'TA' },
  { id: 'm3', name: 'Casey Student', role: 'Student' },
  { id: 'm4', name: 'Sam Student', role: 'Student' },
  { id: 'm5', name: 'Riley Student', role: 'Student' },
  { id: 'm6', name: 'Jamie Student', role: 'Student' },
];

describe('GroupCard with progress', () => {
  it('renders checkpoint progress and members', () => {
    const onOpen = vi.fn();

    render(
      <GroupCard
        name="Progress Group"
        course="CS101"
        checkpoints={checkpoints}
        members={members}
        onOpen={onOpen}
      />,
    );

    expect(screen.getByText('1/2 checkpoints')).toBeInTheDocument();
    expect(screen.getByText('50% complete')).toBeInTheDocument();
    expect(screen.getByText('+1')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Open' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('hides progress section when no checkpoints provided', () => {
    render(
      <GroupCard
        name="No Progress Group"
        course="CS101"
        checkpoints={[]}
        members={[]}
      />,
    );

    expect(screen.queryByText(/checkpoints/)).not.toBeInTheDocument();
  });
});
