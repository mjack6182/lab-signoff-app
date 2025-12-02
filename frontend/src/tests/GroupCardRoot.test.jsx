import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import GroupCard from '../components/GroupCard.jsx';

const members = [
  { id: 'm1', name: 'Alex Teacher', role: 'Teacher' },
  { id: 'm2', name: 'Taylor TA', role: 'TA' },
  { id: 'm3', name: 'Casey Student', role: 'Student' },
  { id: 'm4', name: 'Sam Student', role: 'Student' },
  { id: 'm5', name: 'Riley Student', role: 'Student' },
  { id: 'm6', name: 'Jamie Student', role: 'Student' },
];

describe('GroupCard (root component)', () => {
  it('renders group details and triggers onOpen', () => {
    const onOpen = vi.fn();
    render(
      <GroupCard
        name="Group Alpha"
        course="CS101"
        section="A"
        status="Active"
        updatedAt="2024-02-01T00:00:00Z"
        members={members}
        onOpen={onOpen}
      />,
    );

    expect(screen.getByText('Group Alpha')).toBeInTheDocument();
    expect(screen.getByText('Course:')).toBeInTheDocument();
    expect(screen.getByText('CS101')).toBeInTheDocument();
    expect(screen.getByText('Section:')).toBeInTheDocument();
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText(/Updated/)).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Open' }));
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('shows member overflow indicator and initials', () => {
    render(
      <GroupCard
        name="Overflow Group"
        course="CS101"
        members={members}
      />,
    );

    expect(screen.getByText('+1')).toBeInTheDocument();
    expect(screen.getByText('AT')).toBeInTheDocument();
  });

  it('uses alternative button label when externalUrl exists', () => {
    render(
      <GroupCard
        name="External Group"
        course="CS101"
        members={[]}
        externalUrl="https://example.com"
      />,
    );

    expect(screen.getByRole('button', { name: 'View Lab' })).toBeInTheDocument();
  });
});
