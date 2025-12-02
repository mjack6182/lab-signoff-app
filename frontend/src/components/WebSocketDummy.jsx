import { useState, useEffect } from "react";

export default function WebSocketDummy({ socket }) {
  const [msg, setMsg] = useState("");

  useEffect(() => {
    socket.onmessage = (e) => {
      const data = JSON.parse(e.data);
      setMsg(data.text);
    };
  }, [socket]);

  return <div data-testid="ws-msg">{msg}</div>;
}
