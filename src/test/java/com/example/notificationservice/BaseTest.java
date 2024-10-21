package com.example.notificationservice;

import com.example.notificationservice.config.RedisContainer;
import com.example.notificationservice.model.TransactionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.client.WebGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.security.KeyPair;
import java.util.Date;

import static com.example.notificationservice.config.AuthenticationTestUtility.createJWTToken;
import static com.example.notificationservice.config.AuthenticationTestUtility.mockOauth2JwksEndpoint;
import static io.jsonwebtoken.impl.crypto.RsaProvider.generateKeyPair;

@TestPropertySource(properties = {"spring.config.additional-location=classpath:application-test.yml"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockServerTest({"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:${mockServerPort}/realms/NotificationRealm/protocol/openid-connect/certs"})
@AutoConfigureHttpGraphQlTester
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
    private static final RedisContainer<?> redisContainer = new RedisContainer<>();
    protected WebGraphQlClient webSocketGraphQlClient;
    protected String token;
    protected String expiredToken;
    private KeyPair keyPair;
    protected final String USER_1 = "testuser1";
    protected final String USER_2 = "testuser2";
    private MockServerClient client;
    @Value("http://localhost:${local.server.port}${spring.graphql.websocket.path}")
    private String baseUrl;

    static {
        kafka.start();
        redisContainer.start();
    }

    @Autowired
    protected WebGraphQlTester webGraphQlTester;
    @Autowired
    protected KafkaTemplate<String, byte[]> kafkaTemplate;

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getPort);
    }

    @BeforeAll
    void before() {
        keyPair = generateKeyPair(2048);
        token = createJWTToken(keyPair.getPrivate(), USER_1, new Date(System.currentTimeMillis() + 60 * 1000));
        expiredToken = createJWTToken(keyPair.getPrivate(), USER_1, new Date(System.currentTimeMillis() - 1));
    }

    @BeforeEach
    void setUp() {
        var url = URI.create(baseUrl);
        mockOauth2JwksEndpoint(keyPair, client);
        webSocketGraphQlClient = WebSocketGraphQlClient.builder(url, new ReactorNettyWebSocketClient()).build();
    }

    protected void sendKafkaEventForUser(Long id, String userId, Double amount) {
        var transaction = new TransactionDto(id, userId, amount);
        try {
            kafkaTemplate.setDefaultTopic("my-kafka-topic");
            kafkaTemplate.send(new GenericMessage<>(new ObjectMapper().writeValueAsString(transaction)));
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }
}

