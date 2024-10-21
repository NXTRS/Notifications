package com.example.notificationservice;

import com.example.notificationservice.config.WebsocketClientTestInterceptor;
import com.example.notificationservice.model.NotificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class NotificationsIntegrationTests extends BaseTest {
    private final String QUERY = "query {getUnreadNotifications{transactionId, amount}}";

    @Test
    void getUnreadNotifications() {
        sendKafkaEventForUser(1L, USER_1, 105.5);

        await().untilAsserted(() -> {
            var notifications = doGetUnreadNotificationsCall();
            assertThat(notifications).hasSize(1);
        });
    }

    @Test
    void getUnreadNotificationsNoToken() {
        assertThatThrownBy(() ->
                this.webGraphQlTester
                        .document(QUERY)
                        .executeAndVerify())
                .hasMessage("Status expected:<200 OK> but was:<403 FORBIDDEN>");
    }

    @Test
    void getUnreadNotificationsIncorrectToken() {
        assertThatThrownBy(() ->
                this.webGraphQlTester.mutate()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + "invalid token")
                        .build()
                        .document(QUERY)
                        .executeAndVerify())
                .hasMessage("Status expected:<200 OK> but was:<403 FORBIDDEN>");
    }

    @Test
    void getUnreadNotificationsExpiredToken() {
        assertThatThrownBy(() ->
                this.webGraphQlTester.mutate()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .build()
                        .document(QUERY)
                        .executeAndVerify())
                .hasMessage("Status expected:<200 OK> but was:<401 UNAUTHORIZED>");
    }

    @Test
    void testSubscribeToTransactionNotifications_shouldSucceed() {
        var notificationSubscription = doSubscribeToTransactionNotifications();

        var transactionIdForUser1 = 1L;

        StepVerifier.create(notificationSubscription)
                .thenAwait(Duration.ofSeconds(5))
                .then(() -> {
                    sendKafkaEventForUser(1L, USER_1, 199.5);

                    sendKafkaEventForUser(2L, USER_2, 999D);
                })
                // read one notification, very that the transactionId matches to the one sent to for USER_1 because this
                // user has opened the subscription because it is the only authenticated user
                .assertNext(notification -> assertThat(notification.transactionId()).isEqualTo(transactionIdForUser1))
                .then(() ->
                {   // check that notification will not be returned as unread
                    var notifications = doGetUnreadNotificationsCall();
                    assertThat(notifications).isEmpty();
                })
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }

    private List<NotificationDto> doGetUnreadNotificationsCall() {
        return this.webGraphQlTester
                .mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build()
                .document(QUERY)
                .execute()
                .path("getUnreadNotifications")
                .entityList(NotificationDto.class)
                .get();
    }

    private Flux<NotificationDto> doSubscribeToTransactionNotifications() {
                return this.webSocketGraphQlClient
                .mutate()
                .interceptor(new WebsocketClientTestInterceptor(token, HttpHeaders.AUTHORIZATION))
                .build()
                .document("subscription {subscribeToTransactionNotifications{transactionId, amount}}")
                .retrieveSubscription("subscribeToTransactionNotifications")
                .toEntity(NotificationDto.class);
    }
}
