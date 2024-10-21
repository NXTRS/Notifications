package com.example.notificationservice.config;

import org.springframework.graphql.client.WebSocketGraphQlClientInterceptor;
import reactor.core.publisher.Mono;

import java.util.Map;

public class WebsocketClientTestInterceptor implements WebSocketGraphQlClientInterceptor {
    private final String token;
    private final String authorizationHeaderName;

    public WebsocketClientTestInterceptor(String token, String authorizationHeaderName) {
        this.token = token;
        this.authorizationHeaderName = authorizationHeaderName;
    }

    /**
     *
     * @return
     */
    @Override
    public Mono<Object> connectionInitPayload() {
        return Mono.just(Map.of(authorizationHeaderName, "Bearer " + token));
    }

}
