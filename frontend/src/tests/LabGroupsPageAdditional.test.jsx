import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LabGroups from "../pages/lab-groups/lab-groups";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({ isTeacherOrTA: () => true }) }));
vi.mock("../services/websocketService", () => ({
  websocketService: {
    init: vi.fn(),
    addListener: vi.fn(),
    addStatusListener: vi.fn(),
    subscribeToGroup: vi.fn(),
    unsubscribeFromGroup: vi.fn(),
    removeListener: vi.fn(),
    removeStatusListener: vi.fn(),
    disconnect: vi.fn(),
  },
}));

const makeFetch = (groups) =>
  vi.fn((url) => {
    const str = String(url);
    if (str.includes("/groups")) {
      return Promise.resolve({ ok: true, json: async () => groups });
    }
    if (str.includes("/lti/labs")) {
      return Promise.resolve({ ok: true, json: async () => [{ id: "123", courseId: "CSE101" }] });
    }
    return Promise.resolve({ ok: true, json: async () => ({}) });
  });

describe("LabGroups edge cases", () => {
  it("shows empty state when no groups returned", async () => {
    global.fetch = makeFetch([]);
    render(
      <MemoryRouter initialEntries={["/labs/123/groups"]}>
        <LabGroups />
      </MemoryRouter>
    );

    expect(await screen.findByText(/No groups available/i)).toBeInTheDocument();
  });

  it("renders member counts including zero", async () => {
    const groups = [
      { id: "g1", groupId: "G1", status: "Active", members: [] },
      { id: "g2", groupId: "G2", status: "Active", members: ["Bob"] },
    ];
    global.fetch = makeFetch(groups);

    render(
      <MemoryRouter initialEntries={["/labs/123/groups"]}>
        <LabGroups />
      </MemoryRouter>
    );

    expect(await screen.findByText(/G1/i)).toBeInTheDocument();
    expect(screen.getAllByText(/0 member/).length).toBeGreaterThan(0);
    expect(screen.getByText(/G2/i)).toBeInTheDocument();
    expect(screen.getByText(/1 member/)).toBeInTheDocument();
  });
});
