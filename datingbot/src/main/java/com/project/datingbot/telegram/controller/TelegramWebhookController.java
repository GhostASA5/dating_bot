package com.project.datingbot.telegram.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.datingbot.telegram.service.TelegramHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram/webhook")
public class TelegramWebhookController {

    private final TelegramHandler telegramHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(@RequestBody String rawJson) throws JsonProcessingException {
        Update update = objectMapper.readValue(rawJson, Update.class);

        telegramHandler.handle(update);
    }
}

