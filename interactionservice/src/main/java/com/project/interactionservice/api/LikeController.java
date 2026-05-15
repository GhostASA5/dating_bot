package com.project.interactionservice.api;

import com.project.interactionservice.dto.LikeDto;
import com.project.interactionservice.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
public class LikeController {

    private final InteractionService interactionService;

    @GetMapping("/likes/{telegramId}")
    public List<LikeDto> getLikes(@PathVariable Long telegramId) {
        return interactionService.getLikesReceived(telegramId);
    }
}
