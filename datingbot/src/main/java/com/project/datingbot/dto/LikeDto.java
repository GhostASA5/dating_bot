package com.project.datingbot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LikeDto {
    private Long fromTelegramId;
    private LocalDateTime createdAt;
}
