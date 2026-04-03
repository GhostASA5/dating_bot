package com.project.userservice.entity;

import lombok.Data;

@Data
public class Preferences {
    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private String city;
}