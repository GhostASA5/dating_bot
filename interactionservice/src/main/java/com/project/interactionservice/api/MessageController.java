package com.project.interactionservice.api;

import com.project.interactionservice.dto.DirectMessageDto;
import com.project.interactionservice.dto.SendMessageRequest;
import com.project.interactionservice.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final DirectMessageService directMessageService;

    @PostMapping
    public DirectMessageDto send(@RequestBody SendMessageRequest request) {
        return directMessageService.send(request);
    }

    @GetMapping("/inbox/{telegramId}")
    public List<DirectMessageDto> inbox(@PathVariable Long telegramId) {
        return directMessageService.getInbox(telegramId);
    }
}
