package com.project.datingbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_session")
@Getter
@Setter
public class ChatSession {

    @Id
    private Long telegramId;

    /** Telegram id собеседника, которому пишем следующим сообщением. */
    private Long peerTelegramId;
}
