package com.project.ratingservice.kafka;

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

    public static InteractionEvent parse(String msg) {
        String clean = msg.replace("InteractionEvent(", "").replace(")", "");

        String[] parts = clean.split(", ");

        Long userId = Long.parseLong(parts[0].split("=")[1]);
        Long targetId = Long.parseLong(parts[1].split("=")[1]);
        String type = parts[2].split("=")[1];

        return new InteractionEvent(userId, targetId, type);
    }
}