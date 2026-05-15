package com.project.datingbot.telegram.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.datingbot.telegram.service.TelegramHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram/webhook")
public class TelegramWebhookController {

    private final TelegramHandler telegramHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bot.webhook-secret:}")
    private String webhookSecret;

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(
            @RequestBody String rawJson,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretTokenHeader
    ) throws JsonProcessingException {
        if (!webhookSecret.isBlank() && !webhookSecret.equals(secretTokenHeader)) {
            log.warn("datingbot.webhook: rejected (missing or wrong X-Telegram-Bot-Api-Secret-Token)");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            Update update = objectMapper.readValue(rawJson, Update.class);
            if (update.hasMessage() && update.getMessage().getFrom() != null) {
                MDC.put("telegramId", String.valueOf(update.getMessage().getFrom().getId()));
            }
            log.info("datingbot.webhook: incoming update correlationId={}", correlationId);
            telegramHandler.handle(update);
        } finally {
            MDC.clear();
        }
    }
}

