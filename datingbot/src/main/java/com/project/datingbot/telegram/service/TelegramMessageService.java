package com.project.datingbot.telegram.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${bot.token}")
    private String token;

    @Value("${bot.base-url}")
    private String baseUrl;

    private String apiUrl() {
        return baseUrl + "/bot" + token + "/" + "sendMessage";
    }

    public void sendMessage(String chatId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", escapeMdV2(text));
        body.put("parse_mode", "MarkdownV2");

        try {
            restTemplate.postForObject(apiUrl(), body, String.class);
            log.info("sendMessage ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String respBody = e.getResponseBodyAsString();
            log.warn("sendMessage HTTP error chatId={} status={} body={}", chatId, status, respBody);
            // фолбэк: если Telegram жалуется на парсинг entity — повторяем как plain
            if (status == 400 && respBody.contains("can't parse entities")) {
                log.warn("Retrying sendMessage as plain text due to MarkdownV2 parse error");
                sendPlain(chatId, text);
            }
        } catch (Exception ex) {
            log.warn("sendMessage error chatId={} err={}", chatId, ex.toString());
        }
    }

    public void sendPlain(String chatId, String text) {
        sendPlain(chatId, text, null);
    }

    public void sendPlain(String chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        putReplyMarkup(body, replyMarkup);

        try {
            restTemplate.postForObject(apiUrl(), body, String.class);
            log.info("sendPlain ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            log.warn("sendPlain HTTP error chatId={} status={} body={}", chatId, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("sendPlain error chatId={} err={}", chatId, ex.toString());
        }
    }

    public void sendMainMenu(String chatId, String text) {
        sendPlain(chatId, text, com.project.datingbot.telegram.keyboard.TelegramKeyboards.mainReplyKeyboard());
    }

    public void answerCallbackQuery(String callbackQueryId, String toastText) {
        String url = baseUrl + "/bot" + token + "/answerCallbackQuery";
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (toastText != null && !toastText.isBlank()) {
            body.put("text", toastText);
        }
        try {
            restTemplate.postForObject(url, body, String.class);
        } catch (Exception ex) {
            log.warn("answerCallbackQuery error id={} err={}", callbackQueryId, ex.toString());
        }
    }

    private void putReplyMarkup(Map<String, Object> body, Map<String, Object> replyMarkup) {
        if (replyMarkup == null || replyMarkup.isEmpty()) {
            return;
        }
        try {
            body.put("reply_markup", objectMapper.writeValueAsString(replyMarkup));
        } catch (JsonProcessingException e) {
            log.warn("reply_markup serialization failed", e);
        }
    }

    /** Фото анкеты с подписью и inline-кнопками (sendPhoto). */
    public void sendPhotoWithButtons(String chatId, byte[] photoBytes, String caption, Map<String, Object> replyMarkup) {
        if (photoBytes == null || photoBytes.length == 0) {
            sendWithButtons(chatId, caption, replyMarkup);
            return;
        }

        String url = baseUrl + "/bot" + token + "/sendPhoto";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("caption", caption);
        body.add("photo", new ByteArrayResource(photoBytes) {
            @Override
            public String getFilename() {
                return "avatar.jpg";
            }
        });
        try {
            body.add("reply_markup", objectMapper.writeValueAsString(replyMarkup));
        } catch (Exception e) {
            log.warn("sendPhotoWithButtons: reply_markup serialization failed", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, requestEntity, String.class);
            log.info("sendPhotoWithButtons ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            log.warn("sendPhotoWithButtons HTTP error chatId={} status={} body={}",
                    chatId, e.getStatusCode().value(), e.getResponseBodyAsString());
            sendWithButtons(chatId, caption, replyMarkup);
        } catch (Exception ex) {
            log.warn("sendPhotoWithButtons error chatId={} err={}", chatId, ex.toString());
            sendWithButtons(chatId, caption, replyMarkup);
        }
    }

    /** Текст + inline-кнопки (без Markdown). */
    public void sendPlainInline(String chatId, String text, Map<String, Object> inlineMarkup) {
        sendPlain(chatId, text, inlineMarkup);
    }

    public void sendWithButtons(String chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", escapeMdV2(text));
        body.put("parse_mode", "MarkdownV2");
        putReplyMarkup(body, replyMarkup);

        try {
            restTemplate.postForObject(apiUrl(), body, String.class);
            log.info("sendWithButtons ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            String respBody = e.getResponseBodyAsString();
            log.warn("sendWithButtons HTTP error chatId={} status={} body={}", chatId, e.getStatusCode().value(), respBody);
            // фолбэк: при ошибке MarkdownV2 пробуем plain (без parse_mode)
            if (e.getStatusCode().value() == 400 && respBody.contains("can't parse entities")) {
                Map<String, Object> bodyPlain = new HashMap<>();
                bodyPlain.put("chat_id", chatId);
                bodyPlain.put("text", text);
                putReplyMarkup(bodyPlain, replyMarkup);
                try {
                    restTemplate.postForObject(apiUrl(), bodyPlain, String.class);
                    log.info("sendWithButtons (plain) ok chatId={}", chatId);
                } catch (Exception ignore) { /* fall through */ }
            }
        } catch (Exception ex) {
            log.warn("sendWithButtons error chatId={} err={}", chatId, ex.toString());
        }
    }

    public void sendDocument(String chatId, String classpathFilePath, String caption) {
        String url = baseUrl + "/bot" + token + "/sendDocument";


        ClassPathResource resource = new ClassPathResource(classpathFilePath);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("caption", caption);
        body.add("document", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, requestEntity, String.class);
            log.info("sendDocument ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            log.warn("sendDocument HTTP error chatId={} status={} body={}", chatId, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("sendDocument error chatId={} err={}", chatId, ex.toString());
        }

    }

    public static String escapeMdV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_\\*\\[\\]\\(\\)~`>#+\\-=|{}.!])", "\\\\$1");
    }

    /** Скачивает файл фото по file_id (Bot API getFile + /file/bot...). */
    public byte[] downloadTelegramPhoto(String fileId) {
        String metaUrl = UriComponentsBuilder.fromUriString(baseUrl + "/bot" + token + "/getFile")
                .queryParam("file_id", fileId)
                .build(false)
                .toUriString();
        try {
            String json = restTemplate.getForObject(metaUrl, String.class);
            JsonNode root = objectMapper.readTree(json);
            if (!root.path("ok").asBoolean(false)) {
                log.warn("telegram.getFile: not ok fileId={} body={}", fileId, json);
                throw new IllegalStateException("getFile failed");
            }
            String path = root.path("result").path("file_path").asText(null);
            if (path == null || path.isEmpty()) {
                throw new IllegalStateException("missing file_path");
            }
            String fileUrl = baseUrl + "/file/bot" + token + "/" + path;
            byte[] bytes = restTemplate.getForObject(fileUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("empty file body");
            }
            return bytes;
        } catch (Exception e) {
            log.error("telegram.downloadPhoto: fileId={} err={}", fileId, e.getMessage(), e);
            throw new RuntimeException("downloadTelegramPhoto failed", e);
        }
    }
}
