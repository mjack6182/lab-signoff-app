package com.example.lab_signoff_backend.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LabWebSocketController {

    // Example: client sends to /app/hello
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        return "Hello from backend, you sent: " + message;
    }
}

//frontend react test code
/**
 * import { Client } from "@stomp/stompjs";
 * import SockJS from "sockjs-client";
 *
 * const socket = new SockJS("http://localhost:8080/ws");
 * const stompClient = new Client({
 *   webSocketFactory: () => socket,
 * });
 *
 * stompClient.onConnect = () => {
 *   // Subscribe to server → client messages
 *   stompClient.subscribe("/topic/greetings", (msg) => {
 *     console.log("Received:", msg.body);
 *   });
 *
 *   // Send a test message
 *   stompClient.publish({
 *     destination: "/app/hello",
 *     body: "World",
 *   });
 * };
 *
 * stompClient.activate();
 */