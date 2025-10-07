package com.example.lab_signoff_backend.controller;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class StateNonceStore {
    record Entry(String nonce, Instant exp) {}
    private static final Jedis jedis = new Jedis("localhost", 6379); // Update host/port as needed
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String issueState(String nonce) {
        String state = UUID.randomUUID().toString();
        Entry entry = new Entry(nonce, Instant.now().plusSeconds(300));
        try {
            String entryJson = objectMapper.writeValueAsString(entry);
            jedis.setex("state:" + state, 300, entryJson); // 300 seconds TTL
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize entry", e);
        }
        return state;
    }

    public static Optional<String> consumeNonce(String state) {
        String key = "state:" + state;
        String entryJson = jedis.get(key);
        if (entryJson == null) return Optional.empty();
        try {
            Entry entry = objectMapper.readValue(entryJson, Entry.class);
            if (Instant.now().isAfter(entry.exp())) {
                jedis.del(key);
                return Optional.empty();
            }
            jedis.del(key);
            return Optional.of(entry.nonce());
        } catch (Exception e) {
            jedis.del(key);
            return Optional.empty();
        }
    }
}
