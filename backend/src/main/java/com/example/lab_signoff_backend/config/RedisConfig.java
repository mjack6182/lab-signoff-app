package com.example.lab_signoff_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Configuration class for Redis connection pooling.
 *
 * This class sets up a JedisPool for managing connections to Redis,
 * which is used for storing LTI state and nonce values during the
 * OAuth authentication flow with Canvas LMS.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * The hostname of the Redis server.
     * Defaults to "localhost" if not specified in application properties.
     */
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    /**
     * The port number of the Redis server.
     * Defaults to 6379 (standard Redis port) if not specified in application properties.
     */
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Creates and configures a JedisPool bean for Redis connections.
     *
     * The pool is configured with the following settings:
     * - Maximum total connections: 8
     * - Maximum idle connections: 8
     * - Minimum idle connections: 0
     * - Test connections on borrow, return, and while idle
     *
     * @return A configured JedisPool instance for managing Redis connections
     */
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        return new JedisPool(poolConfig, redisHost, redisPort);
    }
}
