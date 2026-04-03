package com.project.datingbot.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageService {

    private final RestTemplate restTemplate = new RestTemplate();

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
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        try {
            restTemplate.postForObject(apiUrl(), body, String.class);
            log.info("sendPlain ok chatId={}", chatId);
        } catch (RestClientResponseException e) {
            log.warn("sendPlain HTTP error chatId={} status={} body={}", chatId, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("sendPlain error chatId={} err={}", chatId, ex.toString());
        }
    }

    public void sendWithButtons(String chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", escapeMdV2(text));
        body.put("parse_mode", "MarkdownV2");
        body.put("reply_markup", replyMarkup);

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
                bodyPlain.put("reply_markup", replyMarkup);
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
}
