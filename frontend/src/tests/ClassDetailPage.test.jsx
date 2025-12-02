import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";

import ClassDetail from "../pages/class-detail/class-detail";

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
    isTeacherOrTA: () => true,
  }),
}));

describe("ClassDetail page", () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    global.fetch = vi.fn((url) => {
      if (String(url).includes("/api/classes/")) {
        return Promise.resolve({ ok: true, json: async () => ({ id: "cls1", courseName: "Intro CS" }) });
      }
      if (String(url).includes("/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.resolve({ ok: true, json: async () => [] });
    });
  });

  it("renders class info and labs", async () => {
    render(
      <MemoryRouter initialEntries={["/classes/cls1"]}>
        <Routes>
          <Route path="/classes/:classId" element={<ClassDetail />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole("heading", { name: /Intro CS/i })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Labs/i })).toBeInTheDocument();
    expect(await screen.findByText(/No labs are associated with this class yet\./i)).toBeInTheDocument();
    expect(await screen.findByText(/No students are currently in this class\./i)).toBeInTheDocument();
  });
});
