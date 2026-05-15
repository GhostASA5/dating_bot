package com.project.userservice.controller;

import com.project.userservice.dto.UserCreateRequest;
import com.project.userservice.entity.User;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody UserCreateRequest request) {
        log.info("Incoming user: {}", request.getTelegramId());

        userService.createUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        return userService.getAvatarBytes(userId)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/telegram/{telegramId}")
    public ResponseEntity<User> getUserByTelegram(@PathVariable Long telegramId) {
        return userService.getUserByTelegramId(telegramId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/telegram/{telegramId}/avatar")
    public ResponseEntity<Void> uploadAvatar(
            @PathVariable Long telegramId,
            @RequestBody byte[] body
    ) {
        userService.uploadAvatarByTelegramId(telegramId, body);
        return ResponseEntity.ok().build();
    }
}