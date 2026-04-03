package com.project.userservice.dto;

import com.project.userservice.entity.Preferences;
import lombok.Data;

@Data
public class UserCreateRequest {

    private Long telegramId;
    private String username;
    private Integer age;
    private String gender;
    private String city;
    private Preferences preferences;
    private Boolean profileComplete;
}