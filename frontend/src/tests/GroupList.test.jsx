import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, cleanup } from '@testing-library/react';

vi.mock('../config/api', () => ({
  api: {
    groups: vi.fn(),
  },
}));

import { api } from '../config/api';
import GroupList from '../components/GroupList/GroupList';

describe('GroupList', () => {
  beforeEach(() => {
    api.groups.mockReset();
    global.fetch = vi.fn();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('fetches and renders groups', async () => {
    api.groups.mockReturnValue('/groups');
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => [
        { id: 'g1', groupId: 'Group-1', status: 'Active', members: ['A', 'B'] },
      ],
    });

    render(<GroupList />);

    await screen.findByText('Group-1');
    expect(global.fetch).toHaveBeenCalledWith('/groups');
    expect(screen.getByText(/Status:/)).toHaveTextContent('Status: Active');
    expect(screen.getByText(/Members:/)).toHaveTextContent('Members: A, B');
  });

  it('handles fetch errors gracefully', async () => {
    api.groups.mockReturnValue('/groups');
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch.mockResolvedValue({
      ok: false,
      statusText: 'Server error',
    });

    render(<GroupList />);

    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(screen.queryByText(/Status:/)).not.toBeInTheDocument();
    consoleSpy.mockRestore();
  });
});
