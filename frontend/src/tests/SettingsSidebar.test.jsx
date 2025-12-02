import { describe, it, expect, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SettingsSidebar from '../components/SettingsSidebar/SettingsSidebar';

describe('SettingsSidebar', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders navigation links and applies active class', () => {
    render(
      <MemoryRouter initialEntries={['/settings/classes']}>
        <SettingsSidebar />
      </MemoryRouter>,
    );

    const userSettingsLink = screen.getByText('User Settings').closest('a');
    const classesLink = screen.getByText('Classes').closest('a');

    expect(userSettingsLink).toHaveAttribute('href', '/settings');
    expect(classesLink).toHaveAttribute('href', '/settings/classes');
    expect(classesLink).toHaveClass('active');
    expect(userSettingsLink).not.toHaveClass('active');
  });
});
