package com.project.datingbot.service;

import com.project.datingbot.dto.UserCreateRequest;
import com.project.datingbot.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${user.service.url}")
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
}