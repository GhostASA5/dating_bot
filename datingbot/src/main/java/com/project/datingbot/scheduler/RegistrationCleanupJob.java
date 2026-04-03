package com.project.datingbot.scheduler;

import com.project.datingbot.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RegistrationCleanupJob {

    private final RegistrationRepository repository;

    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        repository.deleteAll(
                repository.findAll().stream()
                        .filter(r -> r.getCreatedAt().isBefore(threshold))
                        .toList()
        );
    }
}