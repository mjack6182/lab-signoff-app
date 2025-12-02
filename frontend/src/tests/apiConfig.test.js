import { describe, it, expect, vi } from "vitest";
import { API_BASE_URL, buildApiUrl, api } from "../config/api";

describe("api config", () => {
  it("builds URLs with default base", () => {
    expect(API_BASE_URL).toBe("http://localhost:8080");
    expect(buildApiUrl("/test")).toBe("http://localhost:8080/test");
  });

  it("exposes lab endpoints", () => {
    expect(api.labs()).toContain("/lti/labs");
    expect(api.labs.join()).toContain("/api/labs/join");
    expect(api.labs.byCode("ABC")).toContain("ABC");
    expect(api.ws()).toContain("/ws");
  });

  it("builds class list with params", () => {
    const url = api.classes({ search: "foo" });
    expect(url).toContain("search=foo");
  });
});
