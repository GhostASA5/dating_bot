package com.project.datingbot.service;

import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeedState {

    private final Map<Long, Queue<Long>> feeds = new ConcurrentHashMap<>();

    public void setFeed(Long userId, List<Long> candidates) {
        feeds.put(userId, new LinkedList<>(candidates));
    }

    public Long next(Long userId) {
        Queue<Long> queue = feeds.get(userId);
        return (queue == null) ? null : queue.poll();
    }
}