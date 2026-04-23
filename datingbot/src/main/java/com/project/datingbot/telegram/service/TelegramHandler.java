package com.project.datingbot.telegram.service;


import com.project.datingbot.dto.Preferences;
import com.project.datingbot.dto.UserCreateRequest;
import com.project.datingbot.entity.RegistrationContext;
import com.project.datingbot.entity.RegistrationStep;
import com.project.datingbot.entity.User;
import com.project.datingbot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component
@Slf4j
public class TelegramHandler {

    private final TelegramMessageService telegramService;
    private final RegistrationService registrationService;
    private final UserServiceClient userServiceClient;
    private final InteractionClient interactionClient;
    private final FeedState feedState;
    private final FeedClient feedClient;



    public void handle(Update update) {

        if (update.hasCallbackQuery()) {

            String data = update.getCallbackQuery().getData();
            Long userId = update.getCallbackQuery().getFrom().getId();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

            String[] parts = data.split(":");
            String type = parts[0];
            Long targetId = Long.valueOf(parts[1]);

            interactionClient.send(userId, targetId, type);

            sendNextProfile(chatId, userId);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();

        if (text.equals("/feed")) {
            loadFeed(telegramId, chatId);
            return;
        }

        if (text.equals("/start")) {
            registrationService.start(telegramId);
            telegramService.sendPlain(chatId, "Как тебя зовут?");
            return;
        }

        RegistrationContext ctx = registrationService.get(telegramId);

        if (ctx != null) {
            handleRegistration(chatId, text, ctx);
            return;
        }

        telegramService.sendPlain(chatId, "Напиши /start для регистрации");
    }

    private void handleRegistration(String chatId, String text, RegistrationContext ctx) {

        switch (ctx.getStep()) {

            case USERNAME -> {
                ctx.setUsername(text);
                ctx.setStep(RegistrationStep.AGE);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Сколько тебе лет?");
            }

            case AGE -> {
                try {
                    ctx.setAge(Integer.parseInt(text));
                    ctx.setStep(RegistrationStep.GENDER);
                    registrationService.save(ctx);

                    telegramService.sendPlain(chatId, "Твой пол?");
                } catch (Exception e) {
                    telegramService.sendPlain(chatId, "Введите корректный возраст");
                }
            }

            case GENDER -> {
                ctx.setGender(text);
                ctx.setStep(RegistrationStep.CITY);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Из какого ты города?");
            }

            case CITY -> {
                ctx.setCity(text);
                ctx.setStep(RegistrationStep.PREFERENCES_MIN_AGE);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Минимальный возраст партнёра?");
            }

            case PREFERENCES_MIN_AGE -> {
                ctx.setPrefMinAge(Integer.parseInt(text));
                ctx.setStep(RegistrationStep.PREFERENCES_MAX_AGE);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Максимальный возраст партнёра?");
            }

            case PREFERENCES_MAX_AGE -> {
                ctx.setPrefMaxAge(Integer.parseInt(text));
                ctx.setStep(RegistrationStep.PREFERENCES_GENDER);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Интересующий пол?");
            }

            case PREFERENCES_GENDER -> {
                ctx.setPrefGender(text);
                ctx.setStep(RegistrationStep.PREFERENCES_CITY);
                registrationService.save(ctx);

                telegramService.sendPlain(chatId, "Город поиска?");
            }

            case PREFERENCES_CITY -> {
                ctx.setPrefCity(text);
                registrationService.save(ctx);

                completeRegistration(chatId, ctx);
            }
        }
    }

    private void completeRegistration(String chatId, RegistrationContext ctx) {

        Preferences preferences = new Preferences();
        preferences.setMinAge(ctx.getPrefMinAge());
        preferences.setMaxAge(ctx.getPrefMaxAge());
        preferences.setGender(ctx.getPrefGender());
        preferences.setCity(ctx.getPrefCity());

        UserCreateRequest request = UserCreateRequest.builder()
                .telegramId(ctx.getTelegramId())
                .username(ctx.getUsername())
                .age(ctx.getAge())
                .gender(ctx.getGender())
                .city(ctx.getCity())
                .preferences(preferences)
                .profileComplete(true)
                .build();

        userServiceClient.createUser(request);
        registrationService.delete(ctx.getTelegramId());

        telegramService.sendPlain(chatId, "✅ Регистрация завершена!");
    }

    private void loadFeed(Long userId, String chatId) {

        List<Long> feed = feedClient.getFeed(userId);

        if (feed == null || feed.isEmpty()) {
            telegramService.sendPlain(chatId, "Нет анкет 😢");
            return;
        }

        feedState.setFeed(userId, feed);

        sendNextProfile(chatId, userId);
    }

    private void sendNextProfile(String chatId, Long userId) {

        Long candidateId = feedState.next(userId);

        if (candidateId == null) {
            telegramService.sendPlain(chatId, "Анкеты закончились");
            return;
        }

        User user = userServiceClient.getUser(candidateId);

        String text = formatUser(user);

        telegramService.sendWithButtons(
                chatId,
                text,
                buildButtons(candidateId)
        );
    }

    private String formatUser(User user) {
        return String.format(
                "%s, %d лет\n📍 Проживает %s\n",
                user.getUsername(),
                user.getAge(),
                user.getCity()
        );
    }

    public Map<String, Object> buildButtons(Long targetId) {

        List<List<Map<String, String>>> keyboard = List.of(
                List.of(
                        Map.of("text", "❤️ Like", "callback_data", "LIKE:" + targetId),
                        Map.of("text", "❌ Skip", "callback_data", "SKIP:" + targetId)
                ),
                List.of(
                        Map.of("text", "💬 Message", "callback_data", "MSG:" + targetId)
                )
        );

        return Map.of("inline_keyboard", keyboard);
    }
}