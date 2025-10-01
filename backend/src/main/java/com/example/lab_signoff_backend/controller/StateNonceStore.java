package com.example.lab_signoff_backend.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StateNonceStore {
    record Entry(String nonce, Instant exp) {}
    private static final Map<String, Entry> S = new ConcurrentHashMap<>();
    public static String issueState(String nonce) {
        String state = UUID.randomUUID().toString();
        S.put(state, new Entry(nonce, Instant.now().plusSeconds(300)));
        return state;
    }
    public static Optional<String> consumeNonce(String state) {
        var e = S.remove(state);
        if (e == null || Instant.now().isAfter(e.exp)) return Optional.empty();
        return Optional.of(e.nonce);
    }
}
