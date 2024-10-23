package com.example.notificationservice.config;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableWebSocketSecurity
@AllArgsConstructor
class SecurityConfig {
    private final GraphQlProperties graphQlProperties;

    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        // websocket connections must be excluded because they cannot have security headers,
        // see instead WebSocketAuthenticationInterceptor
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers(graphQlProperties.getWebsocket().getPath()).permitAll())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        http.oauth2Login(Customizer.withDefaults());
        return http.build();
    }
}