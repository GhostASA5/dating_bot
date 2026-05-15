package com.project.interactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeDto {
    private Long fromTelegramId;
    private LocalDateTime createdAt;
}
