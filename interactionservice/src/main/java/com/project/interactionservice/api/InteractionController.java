package com.project.interactionservice.api;

import com.project.interactionservice.kafka.InteractionEvent;
import com.project.interactionservice.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping
    public void interact(@RequestBody InteractionEvent event) {
        interactionService.handle(
                event.getUserId(),
                event.getTargetId(),
                event.getType()
        );
    }
}