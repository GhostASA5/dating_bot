package com.project.datingbot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DirectMessageDto {
    private Long id;
    private Long fromTelegramId;
    private Long toTelegramId;
    private String body;
    private LocalDateTime createdAt;
}
