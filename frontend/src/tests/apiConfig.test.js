import { describe, it, expect, afterEach, vi } from "vitest";

const resetModulesAndEnv = async () => {
  vi.resetModules();
  vi.unstubAllEnvs();
};

describe("api config", () => {
  afterEach(async () => {
    await resetModulesAndEnv();
  });

  it("uses VITE_API_URL when provided", async () => {
    vi.stubEnv("VITE_API_URL", "https://example.test");
    const { API_BASE_URL, buildApiUrl, api } = await import("../config/api");

    expect(API_BASE_URL).toBe("https://example.test");
    expect(buildApiUrl("foo")).toBe("https://example.test/foo");
    expect(api.labDetail("123")).toBe("https://example.test/api/labs/123");
  });

  it("falls back to localhost when VITE_API_URL is missing", async () => {
    const { API_BASE_URL, buildApiUrl } = await import("../config/api");

    expect(API_BASE_URL).toBe("http://localhost:8080");
    expect(buildApiUrl("/api/test")).toBe("http://localhost:8080/api/test");
  });

  it("builds class list URL with query parameters", async () => {
    const { api } = await import("../config/api");
    const url = api.classes({ term: "Fall", section: "001" });
    expect(url).toBe("http://localhost:8080/api/classes?term=Fall&section=001");
  });
});
