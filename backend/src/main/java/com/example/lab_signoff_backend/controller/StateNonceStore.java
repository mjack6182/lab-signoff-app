package com.example.lab_signoff_backend.controller;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * State and nonce storage for LTI OAuth flow.
 *
 * This component manages state-nonce pairs in Redis to prevent replay attacks
 * during the LTI authentication process. State values are generated and stored
 * with their associated nonce, then consumed (deleted) upon validation.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Component
public class StateNonceStore {
    /**
     * Internal record for storing nonce and expiration data.
     *
     * @param nonce The nonce value
     * @param exp   The expiration timestamp
     */
    record Entry(String nonce, Instant exp) {}

    private final JedisPool jedisPool;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor for StateNonceStore.
     *
     * @param jedisPool The Jedis connection pool for Redis operations
     */
    @Autowired
    public StateNonceStore(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * Issues a new state value and stores it with the given nonce.
     *
     * Creates a random UUID state value, associates it with the provided nonce,
     * and stores the mapping in Redis with a 5-minute TTL.
     *
     * @param nonce The nonce to associate with the state
     * @return The generated state value
     * @throws RuntimeException if serialization fails
     */
    public String issueState(String nonce) {
        String state = UUID.randomUUID().toString();
        Entry entry = new Entry(nonce, Instant.now().plusSeconds(300));
        try (Jedis jedis = jedisPool.getResource()) {
            String entryJson = objectMapper.findAndRegisterModules().writeValueAsString(entry);
            jedis.setex("state:" + state, 300, entryJson); // 300 seconds TTL
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize entry", e);
        }
        return state;
    }

    /**
     * Consumes a state value and returns its associated nonce if valid.
     *
     * Retrieves the nonce associated with the given state value, validates
     * that it hasn't expired, and deletes the entry from Redis (preventing reuse).
     *
     * @param state The state value to consume
     * @return Optional containing the nonce if valid, empty otherwise
     */
    public Optional<String> consumeNonce(String state) {
        String key = "state:" + state;
        try (Jedis jedis = jedisPool.getResource()) {
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
}
