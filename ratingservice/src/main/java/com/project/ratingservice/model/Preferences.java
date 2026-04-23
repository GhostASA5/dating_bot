package com.project.ratingservice.model;

import lombok.Data;

@Data
public class Preferences {
    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private String city;
}