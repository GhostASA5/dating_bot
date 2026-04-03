package com.project.datingbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCreateRequest {

    private Long telegramId;
    private String username;
    private Integer age;
    private String gender;
    private String city;
    private Preferences preferences;
    private Boolean profileComplete;
}