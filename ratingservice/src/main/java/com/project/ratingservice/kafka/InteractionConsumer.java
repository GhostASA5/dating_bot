package com.project.ratingservice.kafka;

import com.project.ratingservice.model.Interaction;
import com.project.ratingservice.repository.InteractionRepository;
import com.project.ratingservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionConsumer {

    private final FeedService feedService;
    private final InteractionRepository interactionRepo;

    @KafkaListener(topics = "interactions", groupId = "rating-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String event) {
        try {
            InteractionEvent interaction = InteractionEvent.parse(event);

            interactionRepo.save(
                    Interaction.builder()
                            .userId(interaction.getUserId())
                            .targetId(interaction.getTargetId())
                            .type(interaction.getType())
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            feedService.refreshScoresAfterInteraction(interaction.getUserId(), interaction.getTargetId());
        log.info("rating.kafka: processed interaction actorTelegram={} targetTelegram={} type={}",
                interaction.getUserId(), interaction.getTargetId(), interaction.getType());
        } catch (Exception e) {
            log.error("rating.kafka: failed to process interaction event payload={} err={}", event, e.getMessage(), e);
            throw e;
        }
    }
}