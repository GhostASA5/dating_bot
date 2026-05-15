package com.project.interactionservice.service;

import com.project.interactionservice.dto.LikeDto;
import com.project.interactionservice.kafka.InteractionEvent;
import com.project.interactionservice.kafka.InteractionProducer;
import com.project.interactionservice.model.Interaction;
import com.project.interactionservice.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final InteractionRepository interactionRepo;
    private final InteractionProducer producer;

    public void handle(Long userId, Long targetId, String type) {

        Interaction interaction = Interaction.builder()
                .userId(userId)
                .targetId(targetId)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();

        interactionRepo.save(interaction);

        producer.send(new InteractionEvent(userId, targetId, type));
        log.info("interactionservice: accepted interaction userId={} targetId={} type={}", userId, targetId, type);
    }

    public List<LikeDto> getLikesReceived(Long targetTelegramId) {
        return interactionRepo.findByTargetIdAndTypeOrderByCreatedAtDesc(targetTelegramId, "LIKE").stream()
                .map(i -> new LikeDto(i.getUserId(), i.getCreatedAt()))
                .toList();
    }
}