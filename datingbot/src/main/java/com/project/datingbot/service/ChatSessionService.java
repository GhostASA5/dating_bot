package com.project.datingbot.service;

import com.project.datingbot.entity.ChatSession;
import com.project.datingbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository repository;

    public void startCompose(Long telegramId, Long peerTelegramId) {
        ChatSession session = repository.findById(telegramId).orElse(new ChatSession());
        session.setTelegramId(telegramId);
        session.setPeerTelegramId(peerTelegramId);
        repository.save(session);
    }

    public Long getPeerTelegramId(Long telegramId) {
        return repository.findById(telegramId).map(ChatSession::getPeerTelegramId).orElse(null);
    }

    public void clear(Long telegramId) {
        repository.deleteById(telegramId);
    }
}
