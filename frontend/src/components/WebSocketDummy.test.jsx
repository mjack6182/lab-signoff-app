import { render, screen, act } from "@testing-library/react";
import WebSocketDummy from "./WebSocketDummy";
import { test, expect } from "vitest";

test("component updates when WebSocket receives message", async () => {
  const fakeSocket = {
    onmessage: null,
  };

  render(<WebSocketDummy socket={fakeSocket} />);

  const event = { data: JSON.stringify({ text: "Hello WebSocket" }) };

  await act(async () => {
    fakeSocket.onmessage(event);
  });

  expect(screen.getByTestId("ws-msg")).toHaveTextContent("Hello WebSocket");
});
