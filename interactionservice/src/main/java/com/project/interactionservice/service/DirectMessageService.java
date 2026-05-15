package com.project.interactionservice.service;

import com.project.interactionservice.dto.DirectMessageDto;
import com.project.interactionservice.dto.SendMessageRequest;
import com.project.interactionservice.model.DirectMessage;
import com.project.interactionservice.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectMessageService {

    private final DirectMessageRepository messageRepo;

    public DirectMessageDto send(SendMessageRequest request) {
        if (request.getFromTelegramId() == null || request.getToTelegramId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromTelegramId and toTelegramId required");
        }
        if (request.getFromTelegramId().equals(request.getToTelegramId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }
        String body = request.getBody();
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty message");
        }
        if (body.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long");
        }

        DirectMessage saved = messageRepo.save(DirectMessage.builder()
                .fromTelegramId(request.getFromTelegramId())
                .toTelegramId(request.getToTelegramId())
                .body(body.trim())
                .createdAt(LocalDateTime.now())
                .build());

        log.info("interactionservice.message: from={} to={}", saved.getFromTelegramId(), saved.getToTelegramId());
        return toDto(saved);
    }

    public List<DirectMessageDto> getInbox(Long toTelegramId) {
        return messageRepo.findTop20ByToTelegramIdOrderByCreatedAtDesc(toTelegramId).stream()
                .map(this::toDto)
                .toList();
    }

    private DirectMessageDto toDto(DirectMessage m) {
        return new DirectMessageDto(m.getId(), m.getFromTelegramId(), m.getToTelegramId(), m.getBody(), m.getCreatedAt());
    }
}
