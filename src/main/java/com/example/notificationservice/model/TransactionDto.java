package com.example.notificationservice.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TransactionDto(
    Long id,
    @NotNull(message = "UserId is mandatory.")
    String userId,
    @NotNull(message = "Transaction amount is mandatory.")
    Double amount
){}
