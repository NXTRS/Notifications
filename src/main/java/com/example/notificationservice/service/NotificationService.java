package com.example.notificationservice.service;


import com.example.notificationservice.entity.NotificationEntity;
import com.example.notificationservice.model.NotificationDto;
import com.example.notificationservice.model.TransactionDto;
import com.example.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ChannelTopic channelTopic;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReactiveRedisOperations<String, NotificationEntity> redisTemplate;

    /**
     *
     * @param message - the payload read from the kafka topic
     */
    @KafkaListener(topics = "${spring.kafka.topic}", groupId = "my-group-id")
    public void receiveAndPublishNotifications(@Payload String message) throws JsonProcessingException {
        var transaction = mapper.readValue(message, TransactionDto.class);
        log.trace("Received event with payload [{}]", transaction.toString());
        var notificationEntity = NotificationEntity
                .builder()
                .userId(transaction.userId())
                .transactionId(transaction.id())
                .amount(transaction.amount())
                .received(LocalDateTime.now())
                .read(false)
                .build();

        notificationRepository.save(notificationEntity);
        redisTemplate.convertAndSend(channelTopic.getTopic(), notificationEntity).subscribe();
        log.info("Emitted new transaction event with id [{}] for user with id [{}]",
                notificationEntity.getTransactionId(), notificationEntity.getUserId());
    }

    public List<NotificationDto> getUnreadNotifications(String userId) {
        var notificationList = notificationRepository.getAllByUserIdAndRead(userId, false);

        for (NotificationEntity notificationEntity : notificationList) {
            notificationEntity.setRead(true);
        }

        notificationRepository.saveAll(notificationList);
        log.trace("Retrieved and read [{}] notifications for user with id [{}]", notificationList.size(), userId);

        return notificationList.stream()
                .map(notificationEntity -> new NotificationDto(notificationEntity.getTransactionId(), notificationEntity.getAmount()) )
                .toList();
    }

    public NotificationDto acknowledgeNotification(UUID notificationId) {
        var optional = notificationRepository.findById(notificationId);
        if (optional.isEmpty()) {
            return null;
        }

        var notificationEntity = optional.get();
        notificationEntity.setRead(true);
        notificationRepository.save(notificationEntity);

        return new NotificationDto(notificationEntity.getTransactionId(), notificationEntity.getAmount());
    }
}
