package com.example.lab_signoff_backend.security;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class StateNonceStore {
    private record Entry(String nonce, long expMs) {}
    private static final Map<String, Entry> STORE = new ConcurrentHashMap<>();
    private static final long TTL_MS = 300_000; // 5 min

    public static String issueState(String nonce) {
        String state = UUID.randomUUID().toString();
        STORE.put(state, new Entry(nonce, System.currentTimeMillis() + TTL_MS));
        return state;
    }

    public static Optional<String> consumeNonce(String state) {
        Entry e = STORE.remove(state);                 // one-time use
        if (e == null || System.currentTimeMillis() > e.expMs) return Optional.empty();
        return Optional.of(e.nonce);
    }
}
