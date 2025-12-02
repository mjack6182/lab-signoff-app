import { describe, it, beforeEach, expect, vi } from "vitest";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({
    user: { id: "u1", role: "Teacher", mongoId: "m1", firstName: "Test", lastName: "User", email: "t@test.com" },
    isAuthenticated: true,
    loading: false,
    login: vi.fn(),
    logout: vi.fn(),
    hasCompletedProfile: () => true,
    isTeacherOrTA: () => true,
    isStaffOrAdmin: () => true,
  }),
  AuthProvider: ({ children }) => <>{children}</>,
}));

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../components/SettingsSidebar/SettingsSidebar", () => ({ default: () => <div>Sidebar</div> }));
vi.mock("../services/websocketService", () => {
  const mockService = {
    init: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addStatusListener: vi.fn(),
    removeStatusListener: vi.fn(),
    subscribeToGroup: vi.fn(),
    unsubscribeFromGroup: vi.fn(),
    disconnect: vi.fn(),
  };
  return { __esModule: true, default: mockService, websocketService: mockService };
});

beforeEach(() => {
  global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ([]), text: async () => "" });
});

import Dashboard from "../pages/dashboard/dashboard";
import ClassSelector from "../pages/class-selector/class-selector";
import ClassDetail from "../pages/class-detail/class-detail";
import Checkpoints from "../pages/checkpoints/checkpoints.jsx";
import LabGroups from "../pages/lab-groups/lab-groups";
import LabJoin from "../pages/lab-join/lab-join.jsx";
import Login from "../pages/login/login";
import SelectStudent from "../pages/select-student.jsx/select-student.jsx";
import StudentCheckpoints from "../pages/student-checkpoints/checkpoints.jsx";
import Settings from "../pages/Settings/Settings";
import ClassesSettings from "../pages/Settings/ClassesSettings";
import UserSettings from "../pages/Settings/UserSettings";

const renderPage = (ui) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe("Page smoke tests", () => {
  it("renders Dashboard", () => {
    const { container } = renderPage(<Dashboard />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders ClassSelector", () => {
    const { container } = renderPage(<ClassSelector />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders ClassDetail", () => {
    const { container } = renderPage(<ClassDetail />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders Checkpoints", () => {
    const { container } = renderPage(<Checkpoints />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders LabGroups", () => {
    const { container } = renderPage(<LabGroups />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders LabJoin", () => {
    const { container } = renderPage(<LabJoin />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders Login", () => {
    const { container } = renderPage(<Login />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders SelectStudent", () => {
    const { container } = renderPage(<SelectStudent />);
    expect(container).toBeTruthy();
  });

  it("renders StudentCheckpoints", () => {
    const { container } = renderPage(<StudentCheckpoints />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders Settings", () => {
    const { container } = renderPage(<Settings />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders ClassesSettings", () => {
    const { container } = renderPage(<ClassesSettings />);
    expect(container.firstChild).toBeTruthy();
  });

  it("renders UserSettings", () => {
    const { container } = renderPage(<UserSettings />);
    expect(container.firstChild).toBeTruthy();
  });
});
