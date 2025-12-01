import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SelectStudent from "../pages/select-student.jsx/select-student.jsx";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
  useLocation: () => ({
    state: {
      labCode: "ABC123",
      labId: "lab1",
      labTitle: "Lab 1",
      classId: "cls1",
      className: "Intro CS",
      students: ["Alice", "Bob"],
    },
  }),
}));

vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({}) }));


describe("SelectStudent page", () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        lab: { labId: "lab1", labCode: "ABC123", labTitle: "Lab 1", classId: "cls1", className: "Intro CS", checkpoints: [] },
        group: { id: "g1", groupId: "Group 1" },
      }),
    });
  });

  it("submits selected student and navigates to checkpoints", async () => {
    render(<SelectStudent />);

    expect(screen.getByRole("heading", { name: /Select Your Name/i })).toBeInTheDocument();
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText(/Your Name/i), "Alice");
    await user.click(screen.getByRole("button", { name: /Join Lab/i }));

    expect(global.fetch).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith(
      "/student-checkpoints/lab1/g1",
      expect.anything()
    );
  });
});
