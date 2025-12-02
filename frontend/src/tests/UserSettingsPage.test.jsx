import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import UserSettings from "../pages/Settings/UserSettings";

let mockAuth;
vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => mockAuth,
}));

vi.mock("../components/DeleteAccountModal/DeleteAccountModal", () => ({ default: () => null }));

describe("UserSettings page", () => {
  beforeEach(() => {
    mockAuth = {
      user: { email: "test@example.com", firstName: "Test", lastName: "User" },
      updateUserProfile: vi.fn().mockResolvedValue({}),
    };
  });

  it("shows validation error when names missing", async () => {
    render(<UserSettings />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText(/First Name/i));
    await user.clear(screen.getByLabelText(/Last Name/i));
    await user.click(screen.getByRole("button", { name: /Save Changes/i }));

    await vi.waitFor(() => {
      expect(mockAuth.updateUserProfile).not.toHaveBeenCalled();
    });
    expect(screen.queryByText(/Profile updated successfully/i)).not.toBeInTheDocument();
  });

  it("submits updated profile and shows success", async () => {
    render(<UserSettings />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText(/First Name/i));
    await user.type(screen.getByLabelText(/First Name/i), "New");
    await user.clear(screen.getByLabelText(/Last Name/i));
    await user.type(screen.getByLabelText(/Last Name/i), "Name");
    await user.click(screen.getByRole("button", { name: /Save Changes/i }));

    expect(mockAuth.updateUserProfile).toHaveBeenCalledWith("New", "Name");
    expect(await screen.findByText(/Profile updated successfully/i)).toBeInTheDocument();
  });
});
