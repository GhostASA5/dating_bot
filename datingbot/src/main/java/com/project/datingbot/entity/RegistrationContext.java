package com.project.datingbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_context")
@Getter
@Setter
public class RegistrationContext {

    @Id
    private Long telegramId;

    private String username;
    private Integer age;
    private String gender;
    private String city;

    private Integer prefMinAge;
    private Integer prefMaxAge;
    private String prefGender;
    private String prefCity;

    @Enumerated(EnumType.STRING)
    private RegistrationStep step;

    private LocalDateTime createdAt;
}