import { describe, it, expect, beforeEach, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithRouter } from "./test-utils";
import LabJoin from "../pages/lab-join/lab-join.jsx";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({}) }));
vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));

describe("LabJoin page", () => {
  beforeEach(() => {
    mockNavigate.mockReset();
  });

  it("joins lab successfully and navigates to select-student", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        labCode: "ABC123",
        labId: "lab1",
        labTitle: "Lab 1",
        classId: "cls1",
        className: "Intro CS",
        students: ["Alice", "Bob"],
      }),
    });

    renderWithRouter(<LabJoin />);

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Lab Code/i), "abc123");
    await user.click(screen.getByRole("button", { name: /Continue/i }));

    expect(mockNavigate).toHaveBeenCalledWith("/select-student", expect.anything());
  });

  it("shows error when join fails", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ error: "Unable" }),
    });

    renderWithRouter(<LabJoin />);

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Lab Code/i), "badcode");
    await user.click(screen.getByRole("button", { name: /Continue/i }));

    expect(await screen.findByText(/Unable/i)).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
