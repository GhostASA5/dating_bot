package com.project.datingbot.telegram.service;


import com.project.datingbot.dto.DirectMessageDto;
import com.project.datingbot.dto.LikeDto;
import com.project.datingbot.dto.Preferences;
import com.project.datingbot.dto.UserCreateRequest;
import com.project.datingbot.entity.RegistrationContext;
import com.project.datingbot.entity.RegistrationStep;
import com.project.datingbot.entity.User;
import com.project.datingbot.service.*;
import com.project.datingbot.telegram.keyboard.TelegramKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component
@Slf4j
public class TelegramHandler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final int MAX_INBOX_CARDS = 10;

    private final TelegramMessageService telegramService;
    private final RegistrationService registrationService;
    private final UserServiceClient userServiceClient;
    private final InteractionClient interactionClient;
    private final FeedState feedState;
    private final FeedClient feedClient;
    private final ChatSessionService chatSessionService;

    public void handle(Update update) {

        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (!update.hasMessage()) {
            return;
        }

        Long telegramId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();

        if (update.getMessage().hasPhoto()) {
            handleAvatarUpload(chatId, telegramId, update);
            return;
        }

        if (!update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        String command = commandToken(text);

        if (matchesMenu(text, command, "/feed", "📋 Лента")) {
            loadFeed(telegramId, chatId);
            return;
        }

        if (command.equals("/start")) {
            registrationService.start(telegramId);
            chatSessionService.clear(telegramId);
            telegramService.sendPlain(chatId, "Как тебя зовут?");
            return;
        }

        if (command.equals("/help")) {
            sendHelp(chatId);
            return;
        }

        if (command.equals("/cancel")) {
            cancelCompose(chatId, telegramId);
            return;
        }

        RegistrationContext ctx = registrationService.get(telegramId);
        if (ctx != null) {
            handleRegistration(chatId, text, ctx);
            return;
        }

        if (!isRegistered(telegramId)) {
            telegramService.sendPlain(chatId, "Сначала пройдите регистрацию: /start");
            return;
        }

        if (matchesMenu(text, command, "/likes", "❤️ Лайки")) {
            showLikes(telegramId, chatId);
            return;
        }

        if (matchesMenu(text, command, "/inbox", "📥 Сообщения")) {
            showInbox(telegramId, chatId);
            return;
        }

        Long peerTelegramId = chatSessionService.getPeerTelegramId(telegramId);
        if (peerTelegramId != null) {
            deliverMessage(telegramId, chatId, peerTelegramId, text);
            chatSessionService.clear(telegramId);
            telegramService.sendMainMenu(chatId, "✅ Сообщение отправлено.");
            return;
        }

        telegramService.sendMainMenu(chatId, "Выберите действие кнопками внизу 👇");
    }

    private static boolean matchesMenu(String text, String command, String slashCmd, String buttonLabel) {
        return command.equals(slashCmd) || text.equals(buttonLabel);
    }

    private void handleCallback(Update update) {
        var query = update.getCallbackQuery();
        String data = query.getData();
        Long userTelegramId = query.getFrom().getId();
        String chatId = query.getMessage().getChatId().toString();
        String queryId = query.getId();

        try {
            switch (data) {
                case TelegramKeyboards.CB_CANCEL -> {
                    cancelCompose(chatId, userTelegramId);
                    telegramService.answerCallbackQuery(queryId, "Отменено");
                    return;
                }
                case TelegramKeyboards.CB_MENU_FEED -> {
                    telegramService.answerCallbackQuery(queryId, null);
                    loadFeed(userTelegramId, chatId);
                    return;
                }
                case TelegramKeyboards.CB_MENU_LIKES -> {
                    telegramService.answerCallbackQuery(queryId, null);
                    showLikes(userTelegramId, chatId);
                    return;
                }
                case TelegramKeyboards.CB_MENU_INBOX -> {
                    telegramService.answerCallbackQuery(queryId, null);
                    showInbox(userTelegramId, chatId);
                    return;
                }
                default -> { /* below */ }
            }

            if (data.startsWith(TelegramKeyboards.WRITE_TG_PREFIX)) {
                long peerTelegramId = Long.parseLong(data.substring(TelegramKeyboards.WRITE_TG_PREFIX.length()));
                telegramService.answerCallbackQuery(queryId, "Введите текст сообщения");
                promptCompose(chatId, userTelegramId, peerTelegramId);
                return;
            }

            String[] parts = data.split(":", 2);
            String type = parts[0];
            long refId = Long.parseLong(parts[1]);

            if ("MSG".equals(type)) {
                User target = userServiceClient.getUser(refId);
                if (target == null) {
                    telegramService.answerCallbackQuery(queryId, "Анкета недоступна");
                    telegramService.sendPlain(chatId, "Откройте 📋 Ленту снова.");
                    return;
                }
                telegramService.answerCallbackQuery(queryId, "Введите текст");
                promptCompose(chatId, userTelegramId, target.getTelegramId());
                return;
            }

            User target = userServiceClient.getUser(refId);
            if (target == null) {
                telegramService.answerCallbackQuery(queryId, "Анкета недоступна");
                return;
            }

            if ("LIKE".equals(type)) {
                interactionClient.send(userTelegramId, target.getTelegramId(), "LIKE");
                telegramService.answerCallbackQuery(queryId, "❤️");
                sendNextProfile(chatId, userTelegramId);
                return;
            }

            if ("SKIP".equals(type)) {
                interactionClient.send(userTelegramId, target.getTelegramId(), "SKIP");
                telegramService.answerCallbackQuery(queryId, "Пропущено");
                sendNextProfile(chatId, userTelegramId);
            }
        } catch (Exception e) {
            log.warn("callback error data={} err={}", data, e.getMessage());
            telegramService.answerCallbackQuery(queryId, "Ошибка, попробуйте снова");
        }
    }

    private void showLikes(Long telegramId, String chatId) {
        List<LikeDto> likes = interactionClient.getLikes(telegramId);
        if (likes.isEmpty()) {
            telegramService.sendMainMenu(chatId, "Пока никто не поставил лайк вашей анкете.");
            return;
        }

        StringBuilder sb = new StringBuilder("❤️ Кто лайкнул вас (").append(likes.size()).append("):\n\n");
        List<Map<String, String>> writeBtns = new ArrayList<>();

        int n = 1;
        for (LikeDto like : likes) {
            User liker = userServiceClient.findByTelegramId(like.getFromTelegramId());
            String name = liker != null ? liker.getUsername() + ", " + liker.getAge() : "Пользователь";
            String when = like.getCreatedAt() != null ? like.getCreatedAt().format(TIME_FMT) : "";
            sb.append(n++).append(". ").append(name);
            if (!when.isEmpty()) {
                sb.append(" · ").append(when);
            }
            sb.append("\n");

            String btnLabel = TelegramKeyboards.truncate("💬 " + (liker != null ? liker.getUsername() : "Написать"), 28);
            writeBtns.add(TelegramKeyboards.writeButton(btnLabel, like.getFromTelegramId()));
        }

        sb.append("\nНажмите кнопку, чтобы написать человеку:");
        telegramService.sendPlainInline(
                chatId,
                sb.toString().trim(),
                TelegramKeyboards.inlineKeyboard(TelegramKeyboards.writeButtons(writeBtns))
        );
    }

    private void showInbox(Long telegramId, String chatId) {
        List<DirectMessageDto> inbox = interactionClient.getInbox(telegramId);
        if (inbox.isEmpty()) {
            telegramService.sendMainMenu(chatId, "Входящих сообщений нет.");
            return;
        }

        telegramService.sendPlain(chatId, "📥 Входящие (" + inbox.size() + "). Ответьте кнопкой под сообщением:");

        int count = 0;
        for (DirectMessageDto m : inbox) {
            if (count++ >= MAX_INBOX_CARDS) break;

            User from = userServiceClient.findByTelegramId(m.getFromTelegramId());
            String name = from != null ? from.getUsername() : "Пользователь";
            String when = m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FMT) : "";

            String card = "От " + name + (when.isEmpty() ? "" : " · " + when) + ":\n" + m.getBody();
            telegramService.sendPlainInline(
                    chatId,
                    card,
                    TelegramKeyboards.incomingMessageActions(m.getFromTelegramId(), name)
            );
        }

        if (inbox.size() > MAX_INBOX_CARDS) {
            telegramService.sendMainMenu(chatId, "Показаны последние " + MAX_INBOX_CARDS + " сообщений.");
        }
    }

    private void promptCompose(String chatId, Long telegramId, long peerTelegramId) {
        if (!isRegistered(telegramId)) {
            telegramService.sendPlain(chatId, "Сначала пройдите регистрацию: /start");
            return;
        }
        User peer = userServiceClient.findByTelegramId(peerTelegramId);
        if (peer == null) {
            telegramService.sendMainMenu(chatId, "Пользователь не найден.");
            return;
        }
        chatSessionService.startCompose(telegramId, peerTelegramId);
        telegramService.sendPlainInline(
                chatId,
                "✏️ Напишите сообщение для " + peer.getUsername() + ":",
                TelegramKeyboards.cancelInlineRow()
        );
    }

    private void cancelCompose(String chatId, Long telegramId) {
        chatSessionService.clear(telegramId);
        telegramService.sendMainMenu(chatId, "Действие отменено.");
    }

    private void deliverMessage(Long fromTelegramId, String fromChatId, long toTelegramId, String body) {
        if (userServiceClient.findByTelegramId(toTelegramId) == null) {
            telegramService.sendPlain(fromChatId, "Получатель не найден.");
            return;
        }
        try {
            interactionClient.sendMessage(fromTelegramId, toTelegramId, body);

            User sender = userServiceClient.findByTelegramId(fromTelegramId);
            String senderName = sender != null ? sender.getUsername() : "Кто-то";
            String notify = "💬 Сообщение от " + senderName + ":\n\n" + body;
            telegramService.sendPlainInline(
                    String.valueOf(toTelegramId),
                    notify,
                    TelegramKeyboards.incomingMessageActions(fromTelegramId, senderName)
            );
        } catch (Exception e) {
            log.error("datingbot.message: from={} to={} err={}", fromTelegramId, toTelegramId, e.getMessage(), e);
            telegramService.sendPlain(fromChatId, "Не удалось отправить сообщение. Попробуйте позже.");
        }
    }

    private void sendHelp(String chatId) {
        telegramService.sendMainMenu(chatId, """
                Главное меню — кнопки внизу:
                📋 Лента — анкеты
                ❤️ Лайки — кто лайкнул вас (кнопки «Написать»)
                📥 Сообщения — входящие (кнопка «Ответить»)

                В анкете: ❤️ Like, ❌ Skip, 💬 Написать
                При наборе сообщения: ❌ Отмена
                /cancel — отменить набор""");
    }

    private boolean isRegistered(Long telegramId) {
        return userServiceClient.findByTelegramId(telegramId) != null;
    }

    private static String commandToken(String text) {
        String token = text.split("\\s+", 2)[0];
        int at = token.indexOf('@');
        return (at > 0 ? token.substring(0, at) : token).toLowerCase();
    }

    private void handleAvatarUpload(String chatId, Long telegramId, Update update) {
        if (registrationService.get(telegramId) != null) {
            telegramService.sendPlain(chatId, "Сначала завершите регистрацию, затем можно отправить фото анкеты.");
            return;
        }
        if (!isRegistered(telegramId)) {
            telegramService.sendPlain(chatId, "Сначала пройдите регистрацию: /start");
            return;
        }
        var photos = update.getMessage().getPhoto();
        if (photos == null || photos.isEmpty()) {
            telegramService.sendPlain(chatId, "Не удалось прочитать фото.");
            return;
        }
        String fileId = photos.get(photos.size() - 1).getFileId();
        try {
            byte[] bytes = telegramService.downloadTelegramPhoto(fileId);
            userServiceClient.uploadAvatar(telegramId, bytes);
            telegramService.sendMainMenu(chatId, "Фото анкеты сохранено.");
        } catch (Exception e) {
            log.error("datingbot.avatar: telegramId={} err={}", telegramId, e.getMessage(), e);
            telegramService.sendPlain(chatId, "Ошибка загрузки фото, попробуйте позже.");
        }
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

        telegramService.sendMainMenu(chatId, "✅ Регистрация завершена!\n\nИспользуйте кнопки внизу 👇");
    }

    private void loadFeed(Long userId, String chatId) {

        if (!isRegistered(userId)) {
            telegramService.sendPlain(chatId, "Сначала пройдите регистрацию: /start");
            return;
        }

        List<Long> feed = feedClient.getFeed(userId);

        if (feed == null || feed.isEmpty()) {
            telegramService.sendMainMenu(chatId, "Нет анкет 😢");
            return;
        }

        feedState.setFeed(userId, feed);
        sendNextProfile(chatId, userId);
    }

    private void sendNextProfile(String chatId, Long userId) {

        Long candidateId = feedState.next(userId);

        if (candidateId == null) {
            telegramService.sendMainMenu(chatId, "Анкеты закончились");
            return;
        }

        User user = userServiceClient.getUser(candidateId);
        if (user == null) {
            sendNextProfile(chatId, userId);
            return;
        }

        String text = formatUser(user);
        Map<String, Object> buttons = TelegramKeyboards.feedProfileButtons(candidateId);

        if (user.getAvatarS3Key() != null && !user.getAvatarS3Key().isBlank()) {
            byte[] avatar = userServiceClient.getAvatarBytes(candidateId);
            if (avatar != null && avatar.length > 0) {
                telegramService.sendPhotoWithButtons(chatId, avatar, text, buttons);
                return;
            }
        }

        telegramService.sendPlainInline(chatId, text, buttons);
    }

    private String formatUser(User user) {
        return String.format(
                "%s, %d лет%n📍 %s",
                user.getUsername(),
                user.getAge(),
                user.getCity()
        );
    }
}
