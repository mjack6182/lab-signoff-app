import { describe, it, expect } from "vitest";
import mockCheckpoints from "../mock/checkpoints";

describe("mock checkpoints data", () => {
  it("exports an array of checkpoint definitions", () => {
    expect(Array.isArray(mockCheckpoints)).toBe(true);
    expect(mockCheckpoints.length).toBeGreaterThan(0);
  });

  it("contains checkpoints with required fields", () => {
    const cp = mockCheckpoints[0];
    expect(cp).toHaveProperty("id");
    expect(cp).toHaveProperty("name");
    expect(cp).toHaveProperty("description");
    expect(cp).toHaveProperty("points");
    expect(cp).toHaveProperty("order");
  });
});
