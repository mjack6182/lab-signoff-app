import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";

const createAuthState = () => ({
  user: null,
  hasRole: vi.fn(),
  hasAnyRole: vi.fn(),
  isTeacher: vi.fn(),
  isTA: vi.fn(),
  isStudent: vi.fn(),
  isAdmin: vi.fn(),
  isTeacherOrTA: vi.fn(),
  isStaffOrAdmin: vi.fn(),
});

const mockAuth = createAuthState();

const setAuthState = (overrides = {}) => {
  Object.assign(mockAuth, createAuthState(), overrides);
};

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => mockAuth,
}));

import { RoleGuard, TeacherOnly, StaffOnly } from "../components/RoleGuard/RoleGuard";

beforeEach(() => {
  setAuthState();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("RoleGuard", () => {
  it("shows children when user has a required role", () => {
    setAuthState({
      user: { role: "Teacher" },
      hasAnyRole: vi.fn().mockReturnValue(true),
    });

    render(
      <RoleGuard roles="Teacher">
        <div>Teacher content</div>
      </RoleGuard>,
    );

    expect(screen.getByText("Teacher content")).toBeInTheDocument();
    expect(mockAuth.hasAnyRole).toHaveBeenCalledWith(["Teacher"]);
  });

  it("hides content when no user is present", () => {
    render(
      <RoleGuard roles="Teacher">
        <div>Hidden content</div>
      </RoleGuard>,
    );

    expect(screen.queryByText("Hidden content")).not.toBeInTheDocument();
  });

  it("renders fallback when no user and showFallback is true", () => {
    render(
      <RoleGuard roles="Teacher" fallback={<div>Need login</div>} showFallback>
        <div>Hidden content</div>
      </RoleGuard>,
    );

    expect(screen.getByText("Need login")).toBeInTheDocument();
  });

  it("renders fallback when user lacks role and showFallback is true", () => {
    setAuthState({
      user: { role: "Student" },
      hasAnyRole: vi.fn().mockReturnValue(false),
    });

    render(
      <RoleGuard roles="Teacher" fallback={<div>No access</div>} showFallback>
        <div>Teacher content</div>
      </RoleGuard>,
    );

    expect(screen.getByText("No access")).toBeInTheDocument();
    expect(screen.queryByText("Teacher content")).not.toBeInTheDocument();
  });

  it("renders children when user role is allowed", () => {
    setAuthState({
      user: { role: "Admin" },
      hasAnyRole: vi.fn().mockReturnValue(true),
    });

    render(
      <RoleGuard roles={["Admin", "Teacher"]}>
        <div>Allowed Content</div>
      </RoleGuard>
    );

    expect(screen.getByText("Allowed Content")).toBeInTheDocument();
  });

  it("hides children when user role is not allowed", () => {
    setAuthState({
      user: { role: "Student" },
      hasAnyRole: vi.fn().mockReturnValue(false),
    });

    render(
      <RoleGuard roles={["Admin", "Teacher"]}>
        <div>Hidden Content</div>
      </RoleGuard>
    );

    expect(screen.queryByText("Hidden Content")).not.toBeInTheDocument();
  });
});

describe("Convenience guards", () => {
  it("TeacherOnly normalizes single role and renders for teachers", () => {
    setAuthState({
      user: { role: "Teacher" },
      hasAnyRole: vi.fn().mockReturnValue(true),
    });

    render(
      <TeacherOnly>
        <div>Teacher area</div>
      </TeacherOnly>,
    );

    expect(screen.getByText("Teacher area")).toBeInTheDocument();
    expect(mockAuth.hasAnyRole).toHaveBeenCalledWith(["Teacher"]);
  });

  it("StaffOnly allows any staff role and rejects students", () => {
    setAuthState({
      user: { role: "Teacher" },
      hasAnyRole: vi.fn().mockImplementation((roles) => roles.includes("Teacher")),
    });

    render(
      <StaffOnly>
        <div>Staff area</div>
      </StaffOnly>,
    );

    expect(screen.getByText("Staff area")).toBeInTheDocument();
    expect(mockAuth.hasAnyRole).toHaveBeenCalledWith(["Teacher", "TA"]);

    cleanup();
    setAuthState({
      user: { role: "Student" },
      hasAnyRole: vi.fn().mockReturnValue(false),
    });

    render(
      <StaffOnly fallback={<div>Staff only</div>} showFallback>
        <div>Staff area</div>
      </StaffOnly>,
    );

    expect(screen.getByText("Staff only")).toBeInTheDocument();
    expect(screen.queryByText("Staff area")).not.toBeInTheDocument();
  });
});
