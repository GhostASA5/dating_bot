package com.project.userservice.controller;

import com.project.userservice.dto.UserCreateRequest;
import com.project.userservice.entity.User;
import com.project.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<User> createUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }
}