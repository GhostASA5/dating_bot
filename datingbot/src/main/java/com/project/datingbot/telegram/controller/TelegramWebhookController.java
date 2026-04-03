package com.project.datingbot.telegram.controller;

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

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public void onUpdate(@RequestBody Update update) {
        log.info("Webhook received update: hasMessage={} hasCallback={} updateId={}",
                update.hasMessage(), update.hasCallbackQuery(), update.getUpdateId());

        telegramHandler.handle(update);
    }
}

