package com.project.datingbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedClient {

    private final RestTemplate restTemplate;

    @Value("${rating.service.url:http://localhost:8083}")
    private String ratingServiceUrl;

    public List<Long> getFeed(Long userId) {
        try {
            String url = ratingServiceUrl + "/feed/" + userId;
            return Arrays.asList(restTemplate.getForObject(url, Long[].class));
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }

    }
}