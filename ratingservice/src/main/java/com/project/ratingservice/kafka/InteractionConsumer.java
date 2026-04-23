package com.project.ratingservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ratingservice.model.Interaction;
import com.project.ratingservice.repository.InteractionRepository;
import com.project.ratingservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InteractionConsumer {

    private final FeedService feedService;
    private final InteractionRepository interactionRepo;
    private  final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "interactions", groupId = "rating-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String event) {

        InteractionEvent interaction = InteractionEvent.parse(event);

        interactionRepo.save(
                Interaction.builder()
                        .userId(interaction.getUserId())
                        .targetId(interaction.getTargetId())
                        .type(interaction.getType())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        feedService.onInteraction(interaction.getUserId());
    }
}