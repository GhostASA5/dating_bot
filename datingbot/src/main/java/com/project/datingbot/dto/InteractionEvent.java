package com.project.datingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InteractionEvent {
    private Long userId;
    private Long targetId;
    private String type;
}