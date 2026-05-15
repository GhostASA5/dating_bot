package com.project.datingbot.repository;

import com.project.datingbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
