package com.project.datingbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Long> getFeed(Long userId) {
        String url = "http://localhost:8083/feed/" + userId;
        return Arrays.asList(restTemplate.getForObject(url, Long[].class));
    }
}