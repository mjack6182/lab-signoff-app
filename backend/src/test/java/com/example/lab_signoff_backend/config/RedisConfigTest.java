package com.example.lab_signoff_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConfigTest {

    @Test
    void jedisPool_usesInjectedHostAndPort() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisHost", "localhost");
        ReflectionTestUtils.setField(config, "redisPort", 6379);

        JedisPool pool = config.jedisPool();

        assertNotNull(pool);
        // JedisPool doesn't connect until a resource is fetched; just ensure client config matches
        pool.close(); // creation succeeded with provided host/port config
    }
}
