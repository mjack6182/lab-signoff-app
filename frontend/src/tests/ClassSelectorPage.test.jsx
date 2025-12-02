import { describe, it, expect, beforeEach, vi } from "vitest";
import { screen, fireEvent, within } from "@testing-library/react";
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

let mockAuth;
vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => mockAuth,
}));

describe("ClassSelector page", () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    mockAuth = {
      user: { id: "u1", mongoId: "m1", role: "Teacher" },
      isTeacherOrTA: () => true,
    };
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [
        { id: "c1", courseName: "Intro CS", term: "Fall", section: "001" },
        { id: "c2", courseName: "Data Structures", term: "Spring", section: "002", archived: true },
      ],
    });
  });

  it("shows loading state before data resolves", async () => {
    let resolveFetch;
    global.fetch = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveFetch = resolve;
        }),
    );

    renderWithRouter(<ClassSelector />);
    expect(screen.getByText(/Loading classes/i)).toBeInTheDocument();

    resolveFetch?.({
      ok: true,
      json: async () => [],
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

  it("renders empty state when no classes", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [],
    });

    renderWithRouter(<ClassSelector />);

    expect(await screen.findByText(/No classes available/i)).toBeInTheDocument();
  });

  it("renders error state when fetch fails", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({}),
    });

    renderWithRouter(<ClassSelector />);

    expect(await screen.findByText(/Error loading classes/i)).toBeInTheDocument();
  });

  it("renders empty state when no classes returned", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [],
    });

    renderWithRouter(<ClassSelector />);

    expect(await screen.findByText(/No classes available/i)).toBeInTheDocument();
  });

  it.skip("filters classes by search input (search UI not yet implemented)", () => {
    // TODO: implement search input in class-selector.jsx and update this test.
  });

  it("renders import button for teachers", async () => {
    renderWithRouter(<ClassSelector />);
    expect(await screen.findByText(/Import Gradebook CSV/i)).toBeInTheDocument();
  });

  it("shows import modal and validates non-csv upload", async () => {
    renderWithRouter(<ClassSelector />);

    await userEvent.click(await screen.findByRole("button", { name: /Import Gradebook CSV/i }));
    const dialog = screen.getByRole("dialog");
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByRole("heading", { name: /Import Gradebook CSV/i })).toBeInTheDocument();

    const dropzone = screen.getByRole("button", { name: /Drop your CSV file/i });
    const file = new File(["content"], "notes.txt", { type: "text/plain" });
    fireEvent.drop(dropzone, { dataTransfer: { files: [file] } });

    expect(await screen.findByText(/Please upload a \.csv file/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /Close import modal/i }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("hides import button for students", async () => {
    mockAuth = { user: { role: "Student" }, isTeacherOrTA: () => false };
    renderWithRouter(<ClassSelector />);
    expect(await screen.findByRole("heading", { name: /Classes/i })).toBeInTheDocument();
    expect(screen.queryByText(/Import Gradebook CSV/i)).not.toBeInTheDocument();
  });

  it("retries after error", async () => {
    const originalLocation = window.location;
    const reloadMock = vi.fn();
    // Replace the entire location object to allow mocking reload in JSDOM
    delete window.location;
    window.location = {
      ...originalLocation,
      reload: reloadMock,
    };

    try {
      global.fetch = vi.fn().mockResolvedValueOnce({ ok: false, status: 500, json: async () => ({}) });

      renderWithRouter(<ClassSelector />);
      const retryButton = await screen.findByRole("button", { name: /Retry/i });
      await userEvent.click(retryButton);

      expect(reloadMock).toHaveBeenCalled();
    } finally {
      delete window.location;
      window.location = originalLocation;
    }
  });
});
