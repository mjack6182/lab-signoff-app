import { describe, it, expect, beforeEach, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { render } from "@testing-library/react";
import CheckpointPage from "../pages/checkpoints/checkpoints";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../components/GroupManagementModal", () => ({ default: () => null }));
vi.mock("../components/SignOffModal", () => ({ default: () => null }));
vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({ isTeacher: () => true }) }));
vi.mock("../services/websocketService", () => ({
  websocketService: {
    init: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addStatusListener: vi.fn(),
    removeStatusListener: vi.fn(),
    subscribeToGroup: vi.fn(),
    unsubscribeFromGroup: vi.fn(),
    disconnect: vi.fn(),
  },
}));

const labsResponse = [
  {
    id: "lab123",
    courseId: "CSE101",
    checkpoints: [
      { number: 1, name: "Setup", description: "Desc", points: 1 },
    ],
  },
];

const groupsResponse = [
  {
    id: "g1",
    groupId: "G1",
    status: "Active",
    members: ["Alice"],
    checkpointProgress: [
      { checkpointNumber: 1, status: "PASS", timestamp: "2024-01-01", signedOffByName: "Teacher" },
    ],
  },
];

const fetchMock = vi.fn((url) => {
  const str = String(url);
  if (str.includes("/lti/labs/")) {
    return Promise.resolve({ ok: true, json: async () => groupsResponse });
  }
  if (str.includes("/lti/labs")) {
    return Promise.resolve({ ok: true, json: async () => labsResponse });
  }
  return Promise.resolve({ ok: true, json: async () => ({}) });
});

describe("Teacher checkpoints page", () => {
  beforeEach(() => {
    global.fetch = fetchMock;
  });

  it("renders checkpoints with data", async () => {
    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole("heading", { name: /CSE101/i })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Checkpoints/i })).toBeInTheDocument();
    expect(await screen.findByText(/Setup/i)).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /Undo/i })).toBeInTheDocument();
    const checkpointItem = screen.getByText(/Setup/i).closest(".checkpoint-item");
    expect(checkpointItem).toHaveClass("completed");
  });

  it("renders empty checkpoints state", async () => {
    const emptyFetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/lti/labs/")) {
        return Promise.resolve({ ok: true, json: async () => groupsResponse });
      }
      if (str.includes("/lti/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              id: "lab123",
              courseId: "CSE101",
              checkpoints: [],
            },
          ],
        });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });
    global.fetch = emptyFetch;

    const { container } = render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    const checkpointHeadings = await screen.findAllByText(/Checkpoints/i);
    expect(checkpointHeadings.length).toBeGreaterThan(0);
    const checkpointList = container.querySelector(".checkpoint-list");
    expect(checkpointList).toBeEmptyDOMElement();
  });

  it("renders error state when fetch fails", async () => {
    global.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 500, json: async () => ({}) }));

    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Error loading data:/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Back to Classes/i })).toBeInTheDocument();
  });

  it("handles fetch failure for labs", async () => {
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/lti/labs/")) {
        return Promise.resolve({ ok: true, json: async () => groupsResponse });
      }
      if (str.includes("/lti/labs")) {
        return Promise.resolve({ ok: false, status: 500, json: async () => ({}) });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });

    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Error loading data:/i)).toBeInTheDocument();
  });

  it("renders group info with zero checkpoints", async () => {
    const zeroCheckpointGroups = [
      {
        id: "g1",
        groupId: "G1",
        status: "Active",
        members: ["Alice", "Bob"],
        checkpointProgress: [],
      },
    ];
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/lti/labs/")) {
        return Promise.resolve({ ok: true, json: async () => zeroCheckpointGroups });
      }
      if (str.includes("/lti/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              id: "lab123",
              courseId: "CSE101",
              checkpoints: [],
            },
          ],
        });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });

    const { container } = render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/G1/i)).toBeInTheDocument();
    expect(screen.getByText(/2 members/)).toBeInTheDocument();
    const checkpointList = container.querySelector(".checkpoint-list");
    expect(checkpointList).toBeEmptyDOMElement();
  });

  it("updates checkpoint counts when switching groups", async () => {
    const dualGroups = [
      {
        id: "g1",
        groupId: "G1",
        status: "Active",
        members: ["Alice"],
        checkpointProgress: [{ checkpointNumber: 1, status: "PASS" }],
      },
      {
        id: "g2",
        groupId: "G2",
        status: "Active",
        members: ["Bob"],
        checkpointProgress: [],
      },
    ];
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/lti/labs/")) {
        return Promise.resolve({ ok: true, json: async () => dualGroups });
      }
      if (str.includes("/lti/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [
            { id: "lab123", courseId: "CSE101", checkpoints: [{ number: 1, name: "CP1" }, { number: 2, name: "CP2" }] },
          ],
        });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });

    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/G1/i)).toBeInTheDocument();
    const checkpointsHeading = screen.getByRole("heading", { name: /Checkpoints/i });
    const checkpointsHeader = checkpointsHeading.parentElement ?? document.body;
    expect(within(checkpointsHeader).getAllByText(/1\s*\/\s*2/).length).toBeGreaterThan(0);

    await userEvent.click(screen.getByText(/G2/i));
    expect(within(checkpointsHeader).getAllByText(/0\s*\/\s*2/).length).toBeGreaterThan(0);
  });

  it("shows disconnected status badge", async () => {
    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/DISCONNECTED/i)).toBeInTheDocument();
  });

  it("renders error banner when export fails", async () => {
    const responses = [
      { ok: true, json: async () => labsResponse }, // labs list
      { ok: true, json: async () => groupsResponse }, // groups
      { ok: false, status: 500, text: async () => "export failed" }, // export
    ];
    global.fetch = vi.fn(() => {
      const next = responses.shift();
      return Promise.resolve({
        ok: next.ok,
        status: next.status,
        json: next.json,
        text: next.text,
        blob: next.blob,
      });
    });

    render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByRole("button", { name: /Export Grades \(CSV\)/i });
    await userEvent.click(screen.getByRole("button", { name: /Export Grades \(CSV\)/i }));

    expect(await screen.findByText(/export failed/i)).toBeInTheDocument();
  });

  it("handles no groups returned", async () => {
    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/lti/labs/")) {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      if (str.includes("/lti/labs")) {
        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              id: "lab123",
              courseId: "CSE101",
              checkpoints: [
                { number: 1, name: "CP1" },
              ],
            },
          ],
        });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });

    const { container } = render(
      <MemoryRouter initialEntries={["/labs/lab123/checkpoints"]}>
        <Routes>
          <Route path="/labs/:labId/checkpoints" element={<CheckpointPage />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByRole("heading", { name: /CSE101/i });
    const groupsList = container.querySelector(".groups-list");
    expect(groupsList?.children.length).toBe(0);
    expect(screen.getByText(/Checkpoints/i)).toBeInTheDocument();
    expect(screen.getByText(/0\/1/)).toBeInTheDocument();
  });
});
