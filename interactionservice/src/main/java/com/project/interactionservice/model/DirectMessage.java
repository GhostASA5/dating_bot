package com.project.interactionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "direct_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromTelegramId;
    private Long toTelegramId;

    @Column(length = 4096)
    private String body;

    private LocalDateTime createdAt;
}
