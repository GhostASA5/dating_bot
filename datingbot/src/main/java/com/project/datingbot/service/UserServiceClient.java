package com.project.datingbot.service;

import com.project.datingbot.dto.UserCreateRequest;
import com.project.datingbot.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    public void createUser(UserCreateRequest request) {
        try {
            restTemplate.postForObject(
                    userServiceUrl + "/users",
                    request,
                    Void.class
            );
            log.info("User created: {}", request.getTelegramId());
        } catch (Exception e) {
            log.error("Error creating user", e);
            throw e;
        }
    }

    public User getUser(Long id) {
        return restTemplate.getForObject(
                userServiceUrl + "/users/" + id,
                User.class
        );
    }

    public User findByTelegramId(Long telegramId) {
        try {
            return restTemplate.getForObject(
                    userServiceUrl + "/users/telegram/" + telegramId,
                    User.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public void uploadAvatar(Long telegramId, byte[] imageBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        restTemplate.postForEntity(
                userServiceUrl + "/users/telegram/" + telegramId + "/avatar",
                new HttpEntity<>(imageBytes, headers),
                Void.class
        );
    }

    public byte[] getAvatarBytes(Long userId) {
        try {
            return restTemplate.getForObject(
                    userServiceUrl + "/users/" + userId + "/avatar",
                    byte[].class
            );
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}