package com.project.userservice.service;

import com.project.userservice.dto.UserCreateRequest;
import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.storage.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AvatarStorageService avatarStorageService;

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

    public User getUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public Optional<User> getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Transactional
    public void uploadAvatarByTelegramId(Long telegramId, byte[] body) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (body == null || body.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty body");
        }
        String key = "avatars/" + telegramId + "/" + UUID.randomUUID() + ".jpg";
        avatarStorageService.putObject(key, body, "image/jpeg");
        user.setAvatarS3Key(key);
        userRepository.save(user);
        log.info("userservice.avatar: stored in S3 telegramId={} key={}", telegramId, key);
    }

    public Optional<byte[]> getAvatarBytes(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getAvatarS3Key() == null || user.getAvatarS3Key().isBlank()) {
            return Optional.empty();
        }
        byte[] data = avatarStorageService.getObject(user.getAvatarS3Key());
        return data == null || data.length == 0 ? Optional.empty() : Optional.of(data);
    }
}