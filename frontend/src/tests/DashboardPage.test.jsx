import { describe, it, expect, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithRouter } from "./test-utils";

import DashboardPage from "../pages/dashboard/dashboard";

vi.mock("../components/Header/Header", () => ({ default: () => <div>Header</div> }));

describe("Dashboard page", () => {
  it("shows main heading and stats", () => {
    renderWithRouter(<DashboardPage />);

    expect(screen.getByRole("heading", { name: /CS Lab Dashboard/i })).toBeInTheDocument();
    expect(screen.getByText(/Total Students/i)).toBeInTheDocument();
    expect(screen.getByText(/Pending Check-offs/i)).toBeInTheDocument();
    expect(screen.getByText(/Recent Check-off Activity/i)).toBeInTheDocument();
    expect(screen.getByText(/Quick Actions/i)).toBeInTheDocument();
  });
});
