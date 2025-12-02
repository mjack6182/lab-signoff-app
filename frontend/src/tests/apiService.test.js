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

  it("returns text when content-type is not json", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      text: async () => "ok-text",
      headers: { get: () => "text/plain" },
    });

    const result = await apiService.getLabByCode("abc");
    expect(global.fetch).toHaveBeenCalledWith(
      "https://api/api/labs/code/ABC",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toBe("ok-text");
  });

  it("selectStudent sends payload and returns data", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true }),
      headers: { get: () => "application/json" },
    });

    const result = await apiService.selectStudent("lab1", "Alice", "fp");
    expect(global.fetch).toHaveBeenCalledWith(
      "https://api/api/labs/lab1/select-student",
      expect.objectContaining({
        method: "POST",
      })
    );
    expect(result).toEqual({ success: true });
  });

  it("gets student selections", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ items: [] }),
      headers: { get: () => "application/json" },
    });

    const result = await apiService.getStudentSelections("lab1");
    expect(global.fetch).toHaveBeenCalledWith(
      "https://api/api/labs/lab1/selections",
      expect.objectContaining({ method: "GET" })
    );
    expect(result).toEqual({ items: [] });
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
