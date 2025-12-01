import { describe, it, expect, beforeEach, vi } from "vitest";

vi.mock("../config/api", () => ({
  buildApiUrl: (path) => `https://api${path}`,
}));

import { fetchEnrolledStudents, fetchLabGroups, randomizeGroups, updateGroups, calculateUnassignedStudents } from "../services/groupService";

describe("groupService", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("fetches enrolled students", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ([{ id: 1 }]) });
    const res = await fetchEnrolledStudents("class1");
    expect(global.fetch).toHaveBeenCalledWith("https://api/api/enrollments/class/class1/students?activeOnly=true", { credentials: "include" });
    expect(res).toEqual([{ id: 1 }]);
  });

  it("throws when fetchEnrolledStudents fails", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, statusText: "bad" });
    await expect(fetchEnrolledStudents("class1")).rejects.toThrow(/bad/);
  });

  it("fetches lab groups", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ([{ id: 2 }]) });
    const res = await fetchLabGroups("lab1");
    expect(global.fetch).toHaveBeenCalledWith("https://api/lti/labs/lab1/groups", { credentials: "include" });
    expect(res[0].id).toBe(2);
  });

  it("randomizes groups", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ groups: [] }) });
    const res = await randomizeGroups("lab1");
    expect(global.fetch).toHaveBeenCalledWith("https://api/lti/labs/lab1/randomize-groups", expect.any(Object));
    expect(res.groups).toEqual([]);
  });

  it("updates groups", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ([{ id: 1 }]) });
    const payload = [{ groupId: "g1", members: [] }];
    const res = await updateGroups("lab1", payload);
    expect(global.fetch).toHaveBeenCalledWith("https://api/lti/labs/lab1/groups", expect.objectContaining({ method: "PUT" }));
    expect(res[0].id).toBe(1);
  });

  it("calculates unassigned students", () => {
    const enrolled = [{ userId: "a" }, { userId: "b" }];
    const groups = [{ members: [{ userId: "a" }] }];
    const result = calculateUnassignedStudents(enrolled, groups);
    expect(result).toEqual([{ userId: "b" }]);
  });
});
