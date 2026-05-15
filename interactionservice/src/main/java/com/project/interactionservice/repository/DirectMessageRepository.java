package com.project.interactionservice.repository;

import com.project.interactionservice.model.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findTop20ByToTelegramIdOrderByCreatedAtDesc(Long toTelegramId);
}
