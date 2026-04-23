package com.project.interactionservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InteractionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "interactions";

    public void send(InteractionEvent event) {
        kafkaTemplate.send(TOPIC, event.toString());
    }
}
