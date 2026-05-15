package com.project.ratingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RatingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RatingserviceApplication.class, args);
	}

}
