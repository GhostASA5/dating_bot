package com.project.userservice.service;

import com.project.userservice.dto.UserCreateRequest;
import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public void createUser(UserCreateRequest request) {
        if (userRepository.findByTelegramId(request.getTelegramId()).isPresent()) {
            log.warn("User already exists: {}", request.getTelegramId());
            return;
        }

        User user = User.builder()
                .telegramId(request.getTelegramId())
                .username(request.getUsername())
                .age(request.getAge())
                .gender(request.getGender())
                .city(request.getCity())
                .preferences(request.getPreferences())
                .profileComplete(request.getProfileComplete())
                .build();

        userRepository.save(user);

        log.info("User saved: {}", user.getTelegramId());
    }
}