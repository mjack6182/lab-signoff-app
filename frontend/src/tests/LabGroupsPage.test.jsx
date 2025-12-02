import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import LabGroups from "../pages/lab-groups/lab-groups";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({ isTeacherOrTA: () => true }) }));
vi.mock("../services/websocketService", () => ({
  websocketService: {
    init: vi.fn(),
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
    subscribeToGroup: vi.fn(),
    unsubscribeFromGroup: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addStatusListener: vi.fn(),
    removeStatusListener: vi.fn(),
    disconnect: vi.fn(),
  },
}));

const makeFetch = () =>
  vi.fn((url) => {
    const str = String(url);
    if (str.includes("/groups")) {
      return Promise.resolve({
        ok: true,
        json: async () => [
          { id: "g1", groupId: "G1", status: "Active", members: ["Alice"] },
        ],
      });
    }
    if (str.includes("/lti/labs")) {
      return Promise.resolve({ ok: true, json: async () => [{ id: "123", courseId: "CSE101" }] });
    }
    return Promise.resolve({ ok: true, json: async () => ({}) });
  });

describe("LabGroups page", () => {
  beforeEach(() => {
    global.fetch = makeFetch();
  });

  it("renders groups and members", async () => {
    render(
      <MemoryRouter initialEntries={["/labs/123/groups"]}>
        <Routes>
          <Route path="/labs/:labId/groups" element={<LabGroups />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole("heading", { name: /CSE101/i })).toBeInTheDocument();
    expect(await screen.findByText(/Groups/i)).toBeInTheDocument();
    expect(await screen.findByText(/G1/i)).toBeInTheDocument();
    expect(screen.getByText(/Alice/i)).toBeInTheDocument();
  });
});
