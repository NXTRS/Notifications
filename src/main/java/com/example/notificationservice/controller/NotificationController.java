package com.example.notificationservice.controller;

import com.example.notificationservice.entity.NotificationEntity;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.service.NotificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@Slf4j
@AllArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final ReactiveRedisMessageListenerContainer container;
    private final ChannelTopic channelTopic;
    private final ObjectMapper mapper = new ObjectMapper();

    @QueryMapping("getUnreadNotifications")
    public List<NotificationDto> getUnreadNotifications(Principal principal) {
        log.info("Get unread notifications for user with id [{}]", principal.getName());

        return notificationService.getUnreadNotifications(principal.getName());
    }

    @SubscriptionMapping("subscribeToTransactionNotifications")
    public Flux<NotificationDto> subscribeToTransactionNotifications(Principal principal) {
        log.info("Notifications subscription for user with id [{}]", principal.getName());

        return container.receive(channelTopic)
                .map(ReactiveSubscription.Message::getMessage)
                .mapNotNull(this::mapMessageToNotificationEntity)
                .switchIfEmpty(m -> log.warn("Received incorrectly formatted message: [{}]", m))
                .filter(notification -> String.valueOf(notification.getUserId()).equals(principal.getName()))
                .map(NotificationEntity::getId)
                .map(notificationService::acknowledgeNotification);
    }

    private NotificationEntity mapMessageToNotificationEntity(String message) {
        try {
            return mapper.readValue(message, NotificationEntity.class);
        } catch (IOException e) {
            log.error("Exception on message parsing", e);
            return null;
        }
    }

}
