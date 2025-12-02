import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import StudentCheckpointPage from "../pages/student-checkpoints/checkpoints";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../components/GroupManagementModal", () => ({ default: () => null }));
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

let locationState;
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useNavigate: () => vi.fn(),
    useParams: () => ({ labId: "lab123", groupId: "g1" }),
    useLocation: () => ({ state: locationState }),
  };
});

describe("Student checkpoints page", () => {
  beforeEach(() => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({}) });
    locationState = {
      studentName: "Alice",
      labCode: "ABC123",
      labTitle: "Lab 1",
      className: "Intro CS",
      groupId: "g1",
      groupDisplayId: "G1",
      labData: {
        courseId: "Lab 1",
        checkpoints: [],
      },
      labCheckpoints: [],
      groupData: {
        id: "g1",
        groupId: "G1",
        checkpointProgress: [],
        members: ["Alice"],
      },
    };
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders student checkpoints page shell", async () => {
    const { findByRole, findAllByText, findByText } = render(
      <MemoryRouter>
        <StudentCheckpointPage />
      </MemoryRouter>
    );

    expect(await findByRole("heading", { name: /Lab 1/i })).toBeInTheDocument();
    expect(await findByRole("heading", { name: /Checkpoints/i })).toBeInTheDocument();
    expect(await findByRole("heading", { name: /Your Group/i })).toBeInTheDocument();
    const counts = await findAllByText(/0\s*\/\s*0/);
    expect(counts.length).toBeGreaterThanOrEqual(2);
    expect(await findByText(/0\s*% Complete/i)).toBeInTheDocument();
    expect(await findByRole("button", { name: /Request Help/i })).toBeInTheDocument();
  });

  it("shows error state when fetch fails", async () => {
    global.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 500, json: async () => ({}) }));

    const { findByText } = render(
      <MemoryRouter>
        <StudentCheckpointPage />
      </MemoryRouter>
    );

    expect(await findByText(/Failed to load lab information/i)).toBeInTheDocument();
  });

  // TODO: Re-enable once we finalize the empty-state UI for student checkpoints.
  it.skip("renders empty checkpoints state when the student has no checkpoints", async () => {
    const { findByRole, findAllByText, container } = render(
      <MemoryRouter>
        <StudentCheckpointPage />
      </MemoryRouter>
    );

    expect(await findByRole("heading", { name: /Checkpoints/i })).toBeInTheDocument();
    const counts = await findAllByText(/0\s*\/\s*0/);
    expect(counts.length).toBeGreaterThan(0);
    const checkpointList = container.querySelector(".checkpoint-list");
    expect(checkpointList).toBeEmptyDOMElement();
    expect(await findByRole("heading", { name: /Your Group/i })).toBeInTheDocument();
    expect(await findByText(/0\s*% Complete/i)).toBeInTheDocument();
  });

  it("renders progress with completed and pending checkpoints", async () => {
    const labResponse = {
      id: "lab123",
      courseId: "Lab 1",
      checkpoints: [
        { number: 1, name: "Setup", description: "Desc", points: 1 },
        { number: 2, name: "Build", description: "Desc2", points: 1 },
      ],
    };
    const groupResponse = {
      id: "g1",
      groupId: "G1",
      members: [{ userId: "s1", name: "Alice" }],
      checkpointProgress: [
        { checkpointNumber: 1, status: "PASS", timestamp: "2024-01-02", signedOffByName: "Teacher" },
        { checkpointNumber: 2, status: "PENDING", timestamp: null, signedOffByName: null },
      ],
    };

    global.fetch = vi.fn((url) => {
      const str = String(url);
      if (str.includes("/api/labs/") && !str.includes("/groups/")) {
        return Promise.resolve({ ok: true, json: async () => labResponse });
      }
      if (str.includes("/groups/")) {
        return Promise.resolve({ ok: true, json: async () => groupResponse });
      }
      return Promise.resolve({ ok: true, json: async () => ({}) });
    });

    const { findByText, findAllByText } = render(
      <MemoryRouter>
        <StudentCheckpointPage />
      </MemoryRouter>
    );

    expect(await findByText(/Setup/i)).toBeInTheDocument();
    expect(await findByText(/Build/i)).toBeInTheDocument();
    expect((await findAllByText(/1\s*\/\s*2/)).length).toBeGreaterThan(0);
    expect(await findByText(/50\s*% Complete/i)).toBeInTheDocument();
    expect(await findByText(/Request Help/i)).toBeInTheDocument();
  });
});
