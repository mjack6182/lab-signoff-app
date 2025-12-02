import { describe, it, expect, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithRouter } from "./test-utils";
import Login from "../pages/login/login";

const mockLogin = vi.fn();
const mockNavigate = vi.fn();

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({ login: mockLogin, isAuthenticated: false, loading: false }),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});


describe("Login page", () => {
  it("renders and triggers login", async () => {
    renderWithRouter(<Login />);

    expect(screen.getByRole("heading", { name: /Teacher Portal/i })).toBeInTheDocument();
    const btn = screen.getByRole("button", { name: /Sign in with Auth0/i });
    await userEvent.click(btn);
    expect(mockLogin).toHaveBeenCalled();
  });
});
