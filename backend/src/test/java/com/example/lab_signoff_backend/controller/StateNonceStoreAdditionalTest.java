package com.example.lab_signoff_backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateNonceStoreAdditionalTest {

    @Mock
    private JedisPool pool;
    @Mock
    private Jedis jedis;

    @InjectMocks
    private StateNonceStore store;

    @Test
    void consumeNonce_invalidJsonReturnsEmpty() {
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.get("state:bad")).thenReturn("not json");

        assertTrue(store.consumeNonce("bad").isEmpty());
    }
}
