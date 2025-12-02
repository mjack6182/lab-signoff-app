import { describe, it, expect, vi } from "vitest";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { render, screen } from "@testing-library/react";

const mockAuth = {
  isAuthenticated: false,
  loading: false,
  user: null,
  hasCompletedProfile: () => true,
};

vi.mock("../contexts/AuthContext", () => ({
  AuthProvider: ({ children }) => <>{children}</>,
  useAuth: () => mockAuth,
}));

vi.mock("../components/RoleGuard/RoleGuard", () => ({
  StaffOnly: ({ children }) => <>{children}</>,
}));

vi.mock("../pages/login/login", () => ({ default: () => <div>Login Page</div> }));
vi.mock("../pages/class-selector/class-selector", () => ({ default: () => <div>Class Selector Page</div> }));
vi.mock("../pages/dashboard/dashboard", () => ({ default: () => <div>Dashboard Page</div> }));
vi.mock("../pages/lab-join/lab-join.jsx", () => ({ default: () => <div>Lab Join Page</div> }));
vi.mock("../pages/select-student.jsx/select-student.jsx", () => ({ default: () => <div>Select Student Page</div> }));
vi.mock("../pages/student-checkpoints/checkpoints.jsx", () => ({ default: () => <div>Student Checkpoints Page</div> }));
vi.mock("../pages/checkpoints/checkpoints.jsx", () => ({ default: () => <div>Checkpoint Page</div> }));
vi.mock("../pages/class-detail/class-detail", () => ({ default: () => <div>Class Detail Page</div> }));
vi.mock("../pages/lab-groups/lab-groups", () => ({ default: () => <div>Lab Groups Page</div> }));
vi.mock("../pages/Settings/Settings", () => ({ default: () => <div>Settings Page</div> }));
vi.mock("../pages/Settings/ClassesSettings", () => ({ default: () => <div>Classes Settings Page</div> }));

import App from "../App";

describe("App routing", () => {
  it("redirects unauthenticated users to login", () => {
    mockAuth.isAuthenticated = false;
    mockAuth.user = null;
    render(
      <MemoryRouter initialEntries={["/random"]}>
        <Routes>
          <Route path="*" element={<App />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText("Login Page")).toBeInTheDocument();
  });

  it("renders dashboard for authenticated users", () => {
    mockAuth.isAuthenticated = true;
    mockAuth.user = { id: "u1", role: "Teacher" };
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <Routes>
          <Route path="*" element={<App />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText("Dashboard Page")).toBeInTheDocument();
  });
});
