package com.project.datingbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DatingbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatingbotApplication.class, args);
	}

}
