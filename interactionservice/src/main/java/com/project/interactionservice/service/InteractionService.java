package com.project.interactionservice.service;

import com.project.interactionservice.kafka.InteractionEvent;
import com.project.interactionservice.kafka.InteractionProducer;
import com.project.interactionservice.model.Interaction;
import com.project.interactionservice.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
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
    }
}