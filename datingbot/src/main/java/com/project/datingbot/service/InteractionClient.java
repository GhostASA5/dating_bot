package com.project.datingbot.service;


import com.project.datingbot.dto.InteractionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class InteractionClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(Long userId, Long targetId, String type) {

        InteractionEvent event = new InteractionEvent(userId, targetId, type);

        restTemplate.postForObject(
                "http://localhost:8084/interactions",
                event,
                Void.class
        );
    }
}