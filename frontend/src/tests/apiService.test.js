import { describe, it, expect, beforeEach, vi } from "vitest";

vi.mock("../config/api.js", () => ({
  buildApiUrl: (path) => `https://api${path}`,
  api: {
    labs: {
      join: () => "https://api/api/labs/join",
      roster: (id) => `https://api/api/labs/${id}/roster`,
      byCode: (code) => `https://api/api/labs/code/${code}`,
      selectStudent: (id) => `https://api/api/labs/${id}/select-student`,
      selections: (id) => `https://api/api/labs/${id}/selections`,
    },
  },
}));

import { apiService } from "../services/apiService";

describe("apiService", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("handles a successful JSON response", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ data: "ok" }),
      headers: { get: () => "application/json" },
    });

    const result = await apiService.joinLabWithCode("abc");
    expect(global.fetch).toHaveBeenCalledWith(
      "https://api/api/labs/join",
      expect.objectContaining({ method: "POST" })
    );
    expect(result).toEqual({ data: "ok" });
  });

  it("throws on HTTP error", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: "boom" }),
      headers: { get: () => "application/json" },
    });

    await expect(apiService.getLabRoster("1")).rejects.toThrow(/boom/);
  });
});
