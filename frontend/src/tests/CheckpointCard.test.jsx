import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';

const mockAuth = { user: null, isTeacherOrTA: false };

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockAuth,
}));

import CheckpointCard from '../components/CheckpointCard/CheckpointCard';

const checkpoints = [
  { id: 'c1', name: 'Checkpoint A', completed: false },
  { id: 'c2', name: 'Checkpoint B', completed: true, completedAt: '2024-01-01', completedBy: 'u2' },
];

const members = [
  { id: 'u1', name: 'Alex Teacher' },
  { id: 'u2', name: 'Taylor TA' },
];

describe('CheckpointCard', () => {
  beforeEach(() => {
    Object.assign(mockAuth, {
      user: { id: 'u1', name: 'Alex Teacher' },
      isTeacherOrTA: true,
    });
    vi.useRealTimers();
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it('renders progress and marks view-only when not staff', () => {
    Object.assign(mockAuth, { isTeacherOrTA: false });

    render(
      <CheckpointCard
        groupId="g1"
        groupName="Group One"
        checkpoints={checkpoints}
        members={members}
      />,
    );

    expect(screen.getByText('Group One')).toBeInTheDocument();
    expect(screen.getByText('1/2 checkpoints complete')).toBeInTheDocument();
    expect(screen.getByLabelText('50% complete')).toBeInTheDocument();
    expect(screen.getByText('View Only')).toBeInTheDocument();
  });

  it('calls onCheckpointToggle when staff toggles a checkpoint', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-02-02T12:00:00Z'));
    const onCheckpointToggle = vi.fn();

    render(
      <CheckpointCard
        groupId="g1"
        groupName="Group One"
        checkpoints={checkpoints}
        members={members}
        onCheckpointToggle={onCheckpointToggle}
      />,
    );

    fireEvent.click(screen.getByLabelText('Checkpoint A'));

    expect(onCheckpointToggle).toHaveBeenCalledWith(
      'g1',
      'c1',
      expect.objectContaining({
        id: 'c1',
        completed: true,
        completedBy: 'u1',
        completedAt: '2024-02-02',
      }),
    );
  });

  it('does not toggle checkpoints for non-staff users', () => {
    Object.assign(mockAuth, { isTeacherOrTA: false });
    const onCheckpointToggle = vi.fn();

    render(
      <CheckpointCard
        groupId="g1"
        groupName="Group One"
        checkpoints={checkpoints}
        members={members}
        onCheckpointToggle={onCheckpointToggle}
      />,
    );

    fireEvent.click(screen.getByLabelText('Checkpoint A'));
    expect(onCheckpointToggle).not.toHaveBeenCalled();
  });

  it('renders empty state when no checkpoints exist', () => {
    render(
      <CheckpointCard
        groupId="g1"
        groupName="Empty Group"
        checkpoints={[]}
        members={members}
      />,
    );

    expect(screen.getByText('No checkpoints defined for this group.')).toBeInTheDocument();
  });
});
