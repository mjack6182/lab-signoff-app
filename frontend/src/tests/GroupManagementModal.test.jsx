import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';

vi.mock('../services/groupService', async () => {
  const actual = await vi.importActual('../services/groupService');
  return {
    ...actual,
    fetchEnrolledStudents: vi.fn(),
    fetchLabGroups: vi.fn(),
    randomizeGroups: vi.fn(),
    updateGroups: vi.fn(),
  };
});

import GroupManagementModal from '../components/GroupManagementModal/GroupManagementModal';
import {
  fetchEnrolledStudents,
  fetchLabGroups,
  randomizeGroups,
  updateGroups,
} from '../services/groupService';

const enrolledStudents = [
  { userId: 's1', userName: 'Alice Example', userEmail: 'alice@test.com' },
  { userId: 's2', userName: 'Bob Example', userEmail: 'bob@test.com' },
];

const groupsResponse = [
  {
    id: 'g1',
    groupId: 'Group 1',
    members: [{ userId: 's1', name: 'Alice Example', email: 'alice@test.com' }],
    status: 'FORMING',
    groupNumber: 1,
  },
];

describe('GroupManagementModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('loads groups and unassigned students when opened', async () => {
    fetchEnrolledStudents.mockResolvedValue(enrolledStudents);
    fetchLabGroups.mockResolvedValue(groupsResponse);

    render(
      <GroupManagementModal
        isOpen
        onClose={vi.fn()}
        labId="lab-1"
        classId="class-1"
        labName="Lab 1"
        onUpdateGroups={vi.fn()}
      />,
    );

    expect(await screen.findByText('Group 1')).toBeInTheDocument();
    expect(screen.getByText('Unassigned Students')).toBeInTheDocument();
    expect(screen.getByText('Bob Example')).toBeInTheDocument(); // unassigned
  });

  it('saves groups and notifies parent', async () => {
    fetchEnrolledStudents.mockResolvedValue(enrolledStudents);
    fetchLabGroups.mockResolvedValue(groupsResponse);
    updateGroups.mockResolvedValue([]);
    const onClose = vi.fn();
    const onUpdateGroups = vi.fn();

    render(
      <GroupManagementModal
        isOpen
        onClose={onClose}
        labId="lab-1"
        classId="class-1"
        labName="Lab 1"
        onUpdateGroups={onUpdateGroups}
      />,
    );

    await screen.findByText('Group 1');
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => expect(updateGroups).toHaveBeenCalledTimes(1));
    expect(updateGroups).toHaveBeenCalledWith(
      'lab-1',
      expect.arrayContaining([
        expect.objectContaining({
          groupId: 'Group 1',
          members: expect.any(Array),
        }),
      ]),
    );
    expect(onUpdateGroups).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  it('confirms and executes randomization', async () => {
    fetchEnrolledStudents.mockResolvedValue(enrolledStudents);
    fetchLabGroups.mockResolvedValue(groupsResponse);
    randomizeGroups.mockResolvedValue({
      groups: [
        {
          groupId: 'New Group',
          members: [{ userId: 's2', name: 'Bob Example', email: 'bob@test.com' }],
        },
      ],
    });
    const onUpdateGroups = vi.fn();

    render(
      <GroupManagementModal
        isOpen
        onClose={vi.fn()}
        labId="lab-1"
        classId="class-1"
        labName="Lab 1"
        onUpdateGroups={onUpdateGroups}
      />,
    );

    await screen.findByText('Group 1');
    fireEvent.click(screen.getByRole('button', { name: /Randomize Groups/i }));

    expect(await screen.findByText(/Confirm Randomization/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Yes, Randomize/i }));

    await waitFor(() => expect(randomizeGroups).toHaveBeenCalledWith('lab-1'));
    expect(onUpdateGroups).toHaveBeenCalled();
    expect(await screen.findByText('New Group')).toBeInTheDocument();
  });
});
