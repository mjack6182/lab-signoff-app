import { describe, it, expect, vi } from "vitest";
import { renderWithRouter } from "./test-utils";
import Settings from "../pages/Settings/Settings";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));
vi.mock("../components/SettingsSidebar/SettingsSidebar", () => ({ default: () => <div>Sidebar</div> }));
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, Outlet: () => <div>Outlet content</div> };
});

vi.mock("../contexts/AuthContext", () => ({ useAuth: () => ({}) }));

describe("Settings page", () => {
  it("shows settings layout", () => {
    const { container } = renderWithRouter(<Settings />);
    expect(container.querySelector(".settings-title")?.textContent).toMatch(/Settings/i);
    expect(container).toHaveTextContent("Sidebar");
    expect(container).toHaveTextContent("Outlet content");
  });
});
