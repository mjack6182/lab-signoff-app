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

  it("renders labs when present", async () => {
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/api/classes/cls1/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [{ id: "lab1", title: "Lab One", description: "Desc", checkpoints: [1, 2], status: "Active" }],
        });
      }
      if (str.includes("/api/classes/")) {
        return Promise.resolve({ ok: true, json: async () => ({ id: "cls1", courseName: "Intro CS" }) });
      }
      return Promise.resolve({ ok: true, json: async () => [] });
    });

    render(
      <MemoryRouter initialEntries={["/classes/cls1"]}>
        <Routes>
          <Route path="/classes/:classId" element={<ClassDetail />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Lab One/i)).toBeInTheDocument();
    expect(screen.getByText(/Checkpoints: 2/i)).toBeInTheDocument();
    expect(screen.getByText(/Status: Active/i)).toBeInTheDocument();
  });

  it("renders archived lab status", async () => {
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/api/classes/cls1/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [{ id: "lab1", title: "Lab One", description: "Desc", checkpoints: [], status: "Archived" }],
        });
      }
      if (str.includes("/api/classes/")) {
        return Promise.resolve({ ok: true, json: async () => ({ id: "cls1", courseName: "Intro CS" }) });
      }
      return Promise.resolve({ ok: true, json: async () => [] });
    });

    render(
      <MemoryRouter initialEntries={["/classes/cls1"]}>
        <Routes>
          <Route path="/classes/:classId" element={<ClassDetail />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Lab One/i)).toBeInTheDocument();
    expect(screen.getByText(/Status: Archived/i)).toBeInTheDocument();
  });

  it("renders error state on fetch failure", async () => {
    mockNavigate.mockReset();
    global.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 500, json: async () => ({}) }));

    render(
      <MemoryRouter initialEntries={["/classes/cls1"]}>
        <Routes>
          <Route path="/classes/:classId" element={<ClassDetail />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Failed to load class details/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Back to Classes/i }));
    expect(mockNavigate).toHaveBeenCalledWith("/class-selector");
  });

  it("renders roster when present", async () => {
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/api/classes/cls1/labs")) {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      if (str.includes("/api/classes/")) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ id: "cls1", courseName: "Intro CS", roster: ["Student One", "Student Two"] }),
        });
      }
      return Promise.resolve({ ok: true, json: async () => [] });
    });

    render(
      <MemoryRouter initialEntries={["/classes/cls1"]}>
        <Routes>
          <Route path="/classes/:classId" element={<ClassDetail />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/2 students/i)).toBeInTheDocument();
    expect(screen.getByText(/Student One/i)).toBeInTheDocument();
  });
});
