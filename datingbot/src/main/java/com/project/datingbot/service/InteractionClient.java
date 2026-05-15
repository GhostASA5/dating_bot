package com.project.datingbot.service;

import com.project.datingbot.dto.DirectMessageDto;
import com.project.datingbot.dto.InteractionEvent;
import com.project.datingbot.dto.LikeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InteractionClient {

    private final RestTemplate restTemplate;

    @Value("${interaction.service.url:http://localhost:8084}")
    private String interactionServiceUrl;

    public void send(Long fromTelegramId, Long targetTelegramId, String type) {
        InteractionEvent event = new InteractionEvent(fromTelegramId, targetTelegramId, type);
        restTemplate.postForObject(interactionServiceUrl + "/interactions", event, Void.class);
    }

    public List<LikeDto> getLikes(Long telegramId) {
        LikeDto[] likes = restTemplate.getForObject(
                interactionServiceUrl + "/interactions/likes/" + telegramId,
                LikeDto[].class
        );
        return likes == null ? Collections.emptyList() : List.of(likes);
    }

    public DirectMessageDto sendMessage(Long fromTelegramId, Long toTelegramId, String body) {
        Map<String, Object> payload = Map.of(
                "fromTelegramId", fromTelegramId,
                "toTelegramId", toTelegramId,
                "body", body
        );
        return restTemplate.postForObject(
                interactionServiceUrl + "/messages",
                payload,
                DirectMessageDto.class
        );
    }

    public List<DirectMessageDto> getInbox(Long telegramId) {
        var response = restTemplate.exchange(
                interactionServiceUrl + "/messages/inbox/" + telegramId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DirectMessageDto>>() {}
        );
        return response.getBody() == null ? Collections.emptyList() : response.getBody();
    }
}
