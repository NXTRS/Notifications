package com.example.notificationservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class AuthenticationTestUtility {
    public static String createJWTToken(PrivateKey privateKey, String userId, Date expiration) {
        var claims = new HashMap<String, Object>();
        claims.put("iss", "http://localhost/");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setNotBefore(new Date(System.currentTimeMillis() - 1000))
                .setExpiration(expiration)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
    }

    /**
     * Mocks a successful response when the Keycloak resource server should be called to validate a token for any
     * request
     * Only useful for request that have a token
     */
    public static void mockOauth2JwksEndpoint(KeyPair keyPair, MockServerClient client) {
        var privateKey = keyPair.getPrivate();
        var publicKey = keyPair.getPublic();

        var jwkSet = new JWKSet(List.of(new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey(privateKey)
                .build()));

        client
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/realms/NotificationRealm/protocol/openid-connect/certs"),
                        exactly(1))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(
                                        new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody(jwkSet.toString())
                );
    }
}
