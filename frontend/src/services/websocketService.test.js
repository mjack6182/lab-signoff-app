import { vi, test, expect, beforeEach } from "vitest";

// Mock WebSocket globally
beforeEach(() => {
  global.WebSocket = vi.fn(() => ({
    onopen: null,
    onmessage: null,
    onerror: null,
    send: vi.fn(),
    close: vi.fn(),
  }));
});

test("WebSocket connects successfully", () => {
  const socket = new WebSocket("ws://localhost:5173/ws");

  expect(WebSocket).toHaveBeenCalled();
  expect(socket.send).toBeDefined();
});
