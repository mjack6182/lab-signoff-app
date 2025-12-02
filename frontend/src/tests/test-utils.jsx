import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

export function renderWithRouter(ui, { route = "/", initialEntries } = {}) {
  const entries = initialEntries ?? [route];
  return render(<MemoryRouter initialEntries={entries}>{ui}</MemoryRouter>);
}
