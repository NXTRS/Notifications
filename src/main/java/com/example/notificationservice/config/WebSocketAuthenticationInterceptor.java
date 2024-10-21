package com.example.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * This is an interceptor needed to authenticate websockets connections.
 * It extracts and validates a Bearer token from an init_connection payload
 * and sets it on the SecurityContext
 */
@Component
@Slf4j
public class WebSocketAuthenticationInterceptor implements WebSocketGraphQlInterceptor {

    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-z0-9-._~+/]+=*)$",
            Pattern.CASE_INSENSITIVE
    );

    private final AuthenticationProvider authenticationProvider;

    public WebSocketAuthenticationInterceptor(JwtDecoder jwtDecoder) {
        this.authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
    }

    /**
     * Authenticates a websocket connection when a "connection_init" payload is sent
     * Retrieves the bearer token from the payload
     */
    @NonNull
    @Override
    public Mono<Object> handleConnectionInitialization(@NonNull WebSocketSessionInfo sessionInfo,
                                                       @NonNull Map<String, Object> connectionInitPayload) {
        var sessionHeaders = sessionInfo.getHeaders();

        try {
            var token = resolveTokenFromConnectionInitPayload(connectionInitPayload);
            var bearerTokenAuthenticationToken = new BearerTokenAuthenticationToken(token);
            var authentication = authenticationProvider.authenticate(bearerTokenAuthenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("connection_init successful; sessionId: [{}], userId: [{}], user-agent: {}",
                    sessionInfo.getId(), authentication.getName(), sessionHeaders.get("user-agent"));
        } catch (Exception e) {
            log.error("Exception on connection_init; sessionId: [{}], user-agent: {}",
                    sessionInfo.getId(), sessionHeaders.get("user-agent"), e);
            SecurityContextHolder.getContext().setAuthentication(null);
            return Mono.error(new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED)));
        }

        return Mono.empty();
    }

    private String resolveTokenFromConnectionInitPayload(Map<String, Object> connectionInitPayload) {
        //this will transform the payload in one that ignores case, so that both keys 'Authorization' and 'authorization' can be accepted
        var caseInsensitiveConnectionInitPayload = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveConnectionInitPayload.putAll(connectionInitPayload);

        var authorization = Optional.ofNullable(caseInsensitiveConnectionInitPayload.get(HttpHeaders.AUTHORIZATION))
                .map(Object::toString)
                .orElseThrow(() -> new OAuth2AuthenticationException("Missing authorization token"));

        if (!StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
            var error = BearerTokenErrors.invalidToken("Bearer token is required");
            throw new OAuth2AuthenticationException(error);
        }

        var matcher = authorizationPattern.matcher(authorization);
        if (!matcher.matches()) {
            var error = BearerTokenErrors.invalidToken("Bearer token is invalid");
            throw new OAuth2AuthenticationException(error);
        }

        return matcher.group("token");
    }
}
