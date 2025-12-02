import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import ClassesSettings from "../pages/Settings/ClassesSettings";

let mockAuth;
vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => mockAuth,
}));

describe("ClassesSettings page", () => {
  beforeEach(() => {
    mockAuth = { user: { role: "Student" } };
  });

  it("denies access for non-staff", () => {
    render(<ClassesSettings />);

    expect(screen.getByText(/You do not have permission/i)).toBeInTheDocument();
  });

  it("shows coming soon state for staff", () => {
    mockAuth = { user: { role: "Teacher" } };
    render(<ClassesSettings />);

    expect(screen.getByText(/Class Management Coming Soon/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Create Class/i })).toBeDisabled();
  });
});
