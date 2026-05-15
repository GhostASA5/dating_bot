package com.project.datingbot.telegram.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TelegramKeyboards {

    private TelegramKeyboards() {
    }

    public static final String CB_CANCEL = "CANCEL";
    public static final String CB_MENU_FEED = "MENU_FEED";
    public static final String CB_MENU_LIKES = "MENU_LIKES";
    public static final String CB_MENU_INBOX = "MENU_INBOX";

    public static final String WRITE_TG_PREFIX = "WRITE_TG:";
    public static final String LIKE_PREFIX = "LIKE:";
    public static final String SKIP_PREFIX = "SKIP:";
    public static final String MSG_PREFIX = "MSG:";

    /** Постоянное меню внизу чата. */
    public static Map<String, Object> mainReplyKeyboard() {
        return Map.of(
                "keyboard", List.of(
                        List.of(
                                Map.of("text", "📋 Лента"),
                                Map.of("text", "❤️ Лайки")
                        ),
                        List.of(
                                Map.of("text", "📥 Сообщения")
                        )
                ),
                "resize_keyboard", true
        );
    }

    public static Map<String, Object> cancelInlineRow() {
        return Map.of(
                "inline_keyboard", List.of(
                        List.of(Map.of("text", "❌ Отмена", "callback_data", CB_CANCEL))
                )
        );
    }

    public static Map<String, Object> inlineKeyboard(List<List<Map<String, String>>> rows) {
        return Map.of("inline_keyboard", rows);
    }

    /** До 2 кнопок в ряд для списка «Написать …». */
    public static List<List<Map<String, String>>> writeButtons(List<Map<String, String>> buttons) {
        List<List<Map<String, String>>> rows = new ArrayList<>();
        List<Map<String, String>> row = new ArrayList<>();
        for (Map<String, String> btn : buttons) {
            row.add(btn);
            if (row.size() == 2) {
                rows.add(List.copyOf(row));
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        rows.add(List.of(Map.of("text", "📋 В меню", "callback_data", CB_MENU_FEED)));
        return rows;
    }

    public static Map<String, String> writeButton(String label, long peerTelegramId) {
        return Map.of(
                "text", label,
                "callback_data", WRITE_TG_PREFIX + peerTelegramId
        );
    }

    public static Map<String, Object> feedProfileButtons(long userDbId) {
        return inlineKeyboard(List.of(
                List.of(
                        Map.of("text", "❤️ Like", "callback_data", LIKE_PREFIX + userDbId),
                        Map.of("text", "❌ Skip", "callback_data", SKIP_PREFIX + userDbId)
                ),
                List.of(
                        Map.of("text", "💬 Написать", "callback_data", MSG_PREFIX + userDbId)
                )
        ));
    }

    public static Map<String, Object> incomingMessageActions(long fromTelegramId, String senderLabel) {
        String label = truncate("↩️ " + senderLabel, 32);
        return inlineKeyboard(List.of(
                List.of(Map.of("text", label, "callback_data", WRITE_TG_PREFIX + fromTelegramId)),
                List.of(
                        Map.of("text", "📥 Все сообщения", "callback_data", CB_MENU_INBOX),
                        Map.of("text", "📋 Лента", "callback_data", CB_MENU_FEED)
                )
        ));
    }

    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
