package com.example.notificationservice.model;

public record NotificationDto(
        Long transactionId,
        Double amount
) {}
