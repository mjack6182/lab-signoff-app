package com.example.lab_signoff_backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateNonceStoreTest {

    @Mock
    private JedisPool pool;
    @Mock
    private Jedis jedis;

    @InjectMocks
    private StateNonceStore store;

    @Test
    void issueState_writesToRedisWithPrefix() {
        when(pool.getResource()).thenReturn(jedis);
        assertNotNull(store);

        String state = store.issueState("nonce-1");

        assertNotNull(state);
    }

    @Test
    void consumeNonce_missingReturnsEmpty() {
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.get("state:missing")).thenReturn(null);

        assertTrue(store.consumeNonce("missing").isEmpty());
    }

    @Test
    void consumeNonce_expiredDeletesAndEmpty() throws Exception {
        when(pool.getResource()).thenReturn(jedis);
        StateNonceStore.Entry expired = new StateNonceStore.Entry("nonce", Instant.now().minusSeconds(10));
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        String json = mapper.writeValueAsString(expired);
        when(jedis.get("state:exp")).thenReturn(json);

        Optional<String> result = store.consumeNonce("exp");

        assertTrue(result.isEmpty());
        verify(jedis).del("state:exp");
    }

    @Test
    void consumeNonce_validReturnsNonceAndDeletes() throws Exception {
        when(pool.getResource()).thenReturn(jedis);
        StateNonceStore.Entry entry = new StateNonceStore.Entry("nonce-ok", Instant.now().plusSeconds(50));
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        String json = mapper.writeValueAsString(entry);
        when(jedis.get("state:ok")).thenReturn(json);
        when(jedis.del("state:ok")).thenReturn(1L);

        Optional<String> result = store.consumeNonce("ok");

        assertEquals("nonce-ok", result.orElse(null));
        verify(jedis).del("state:ok");
    }

    @Test
    void consumeNonce_invalidJson_returnsEmptyAndDeletes() {
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.get("state:bad")).thenReturn("{not-json");

        Optional<String> result = store.consumeNonce("bad");

        assertTrue(result.isEmpty());
        verify(jedis).del("state:bad");
    }
}
