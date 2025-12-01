import { describe, it, expect, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import SignOffModal from '../components/SignOffModal/SignOffModal';

const checkpoint = {
  name: 'Checkpoint 1',
  description: 'Do the thing',
  points: 10,
};

describe('SignOffModal', () => {
  afterEach(() => {
    cleanup();
  });

  it('does not render when closed or no checkpoint', () => {
    const { container } = render(
      <SignOffModal
        isOpen={false}
        selectedCheckpoint={checkpoint}
        signOffStatus="pass"
        signOffNotes=""
        setSignOffNotes={vi.fn()}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );

    expect(container.firstChild).toBeNull();
  });

  it('renders content for sign off and updates notes', () => {
    const setSignOffNotes = vi.fn();
    render(
      <SignOffModal
        isOpen
        selectedCheckpoint={checkpoint}
        signOffStatus="pass"
        signOffNotes=""
        setSignOffNotes={setSignOffNotes}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );

    expect(screen.getByRole('heading', { name: /Sign Off/i })).toBeInTheDocument();
    expect(screen.getByText('Confirm Sign Off')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Notes'), { target: { value: 'Great work' } });
    expect(setSignOffNotes).toHaveBeenCalledWith('Great work');
  });

  it('closes when clicking the overlay', () => {
    const onClose = vi.fn();
    const { container } = render(
      <SignOffModal
        isOpen
        selectedCheckpoint={checkpoint}
        signOffStatus="undo"
        signOffNotes=""
        setSignOffNotes={vi.fn()}
        onClose={onClose}
        onConfirm={vi.fn()}
      />,
    );

    fireEvent.click(container.firstChild);
    expect(onClose).toHaveBeenCalled();
  });
});
