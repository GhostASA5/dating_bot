package com.project.interactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessageDto {
    private Long id;
    private Long fromTelegramId;
    private Long toTelegramId;
    private String body;
    private LocalDateTime createdAt;
}
