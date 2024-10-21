package com.example.notificationservice.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer<SELF extends RedisContainer<SELF>> extends GenericContainer<SELF> {
    private static final int PORT = 6379;

    public RedisContainer() {
        super(DockerImageName.parse("redis").withTag("7.0.12-alpine"));
        this.waitStrategy = Wait.forLogMessage(".*Ready to accept connections.*\\n", 1);
        this.addExposedPort(PORT);
    }

    public Integer getPort() {
        return this.getMappedPort(PORT);
    }
}
