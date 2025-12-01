import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { render, waitFor } from "@testing-library/react";
import React, { useEffect } from "react";
import { AuthProvider, useAuth } from "../contexts/AuthContext.jsx";

let mockAuth0;

vi.mock("@auth0/auth0-react", () => ({
  useAuth0: () => mockAuth0,
}));

const mockFetchSuccess = {
  ok: true,
  json: async () => ({
    success: true,
    user: {
      id: "mongo-1",
      primaryRole: "Admin",
      firstName: "Backend",
      lastName: "User",
      role: "Admin",
    },
  }),
  headers: { get: () => "application/json" },
};

const mockFetchFail = {
  ok: false,
  statusText: "fail",
  json: async () => ({})
};

const setupFetch = (impl) => {
  global.fetch = vi.fn(impl);
};

let capturedAuth;

const CaptureAuth = ({ onReady }) => {
  const auth = useAuth();
  useEffect(() => {
    onReady(auth);
  }, [auth, onReady]);
  return null;
};

const renderWithProvider = async (options = {}) => {
  capturedAuth = undefined;
  render(
    <AuthProvider>
      <CaptureAuth onReady={(value) => (capturedAuth = value)} />
    </AuthProvider>
  );
  await waitFor(() => expect(capturedAuth).toBeTruthy());
  if (options.waitForUser) {
    await waitFor(() => expect(capturedAuth.user).toBeTruthy());
  }
  return capturedAuth;
};

describe("AuthContext", () => {
  beforeEach(() => {
    mockAuth0 = {
      user: {
        sub: "auth0|123",
        name: "Test User",
        email: "test@example.com",
        given_name: "Test",
        family_name: "User",
        picture: "pic",
        "https://lab-signoff-app/roles": ["Teacher"],
      },
      isAuthenticated: true,
      isLoading: false,
      loginWithRedirect: vi.fn(),
      logout: vi.fn(),
      getAccessTokenSilently: vi.fn(),
    };
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("syncs user from backend when available", async () => {
    setupFetch(() => Promise.resolve(mockFetchSuccess));
    const auth = await renderWithProvider({ waitForUser: true });

    expect(auth.user).toMatchObject({
      id: "auth0|123",
      role: "Admin",
      firstName: "Backend",
      lastName: "User",
    });
    expect(auth.hasRole("Admin")).toBe(true);
    expect(auth.isStaffOrAdmin()).toBe(true);
  });

  it("falls back to auth0 role when backend fails", async () => {
    setupFetch(() => Promise.resolve(mockFetchFail));
    const auth = await renderWithProvider({ waitForUser: true });

    expect(auth.user).toMatchObject({
      role: "Teacher",
      firstName: "Test",
      lastName: "User",
    });
    expect(auth.hasAnyRole(["Teacher"])).toBe(true);
  });

  it("switchRole logs a warning but leaves role unchanged", async () => {
    setupFetch(() => Promise.resolve(mockFetchSuccess));
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    const auth = await renderWithProvider({ waitForUser: true });

    auth.switchRole("TA");
    expect(auth.user.role).toBe("Admin");
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });

  it("updateUserProfile sends update request; user state remains unchanged immediately", async () => {
    setupFetch((url) => {
      if (String(url).includes("auth/sync")) return Promise.resolve(mockFetchSuccess);
      return Promise.resolve({
        ok: true,
        json: async () => ({ success: true, user: { firstName: "New", lastName: "Name", name: "New Name" } }),
        headers: { get: () => "application/json" },
      });
    });

    const auth = await renderWithProvider({ waitForUser: true });
    await auth.updateUserProfile("New", "Name");
    expect(global.fetch).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/api/users/profile"),
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ auth0Id: "auth0|123", firstName: "New", lastName: "Name" }),
      })
    );
    expect(auth.user.firstName).toBe("Backend");
    expect(auth.user.lastName).toBe("User");
  });

  it("deleteAccount triggers logout after success", async () => {
    setupFetch((url) => {
      if (String(url).includes("auth/sync")) return Promise.resolve(mockFetchSuccess);
      return Promise.resolve({ ok: true, json: async () => ({ success: true }), headers: { get: () => "application/json" } });
    });

    const auth = await renderWithProvider({ waitForUser: true });
    await auth.deleteAccount();
    expect(mockAuth0.logout).toHaveBeenCalled();
  });

  it("hasCompletedProfile checks names", async () => {
    setupFetch(() => Promise.resolve(mockFetchSuccess));
    const auth = await renderWithProvider({ waitForUser: true });
    expect(auth.hasCompletedProfile()).toBe(true);
  });
});
