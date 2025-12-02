import { describe, it, expect, beforeEach, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithRouter } from "./test-utils";
import ClassSelector from "../pages/class-selector/class-selector";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({
    user: { id: "u1", mongoId: "m1", role: "Teacher" },
    isTeacherOrTA: () => true,
  }),
}));

describe("ClassSelector page", () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [
        { id: "c1", courseName: "Intro CS", term: "Fall", section: "001" },
        { id: "c2", courseName: "Data Structures", term: "Spring", section: "002", archived: true },
      ],
    });
  });

  it("renders classes and opens class detail", async () => {
    renderWithRouter(<ClassSelector />);

    expect(await screen.findByRole("heading", { name: /Classes/i })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Intro CS/i })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Data Structures/i })).toBeInTheDocument();

    await userEvent.click(screen.getAllByRole("button", { name: /View Labs/i })[0]);
    expect(mockNavigate).toHaveBeenCalledWith("/classes/c1");
  });
});
