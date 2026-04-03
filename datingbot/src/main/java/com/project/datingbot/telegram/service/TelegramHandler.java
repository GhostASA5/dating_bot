package com.project.datingbot.telegram.service;


import com.project.datingbot.dto.Preferences;
import com.project.datingbot.dto.UserCreateRequest;
import com.project.datingbot.entity.RegistrationContext;
import com.project.datingbot.entity.RegistrationStep;
import com.project.datingbot.service.RegistrationService;
import com.project.datingbot.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;


@RequiredArgsConstructor
@Component
@Slf4j
public class TelegramHandler {

    private final TelegramMessageService telegramService;
    private final RegistrationService registrationService;
    private final UserServiceClient userServiceClient;

    public void handle(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();

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
}