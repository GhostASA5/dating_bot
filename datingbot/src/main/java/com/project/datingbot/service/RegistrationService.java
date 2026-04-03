package com.project.datingbot.service;

import com.project.datingbot.entity.RegistrationContext;
import com.project.datingbot.entity.RegistrationStep;
import com.project.datingbot.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RegistrationRepository repository;

    public RegistrationContext start(Long telegramId) {
        RegistrationContext entity = new RegistrationContext();
        entity.setTelegramId(telegramId);
        entity.setStep(RegistrationStep.USERNAME);
        entity.setCreatedAt(LocalDateTime.now());

        return repository.save(entity);
    }

    public RegistrationContext get(Long telegramId) {
        return repository.findById(telegramId).orElse(null);
    }

    public RegistrationContext save(RegistrationContext entity) {
        return repository.save(entity);
    }

    public void delete(Long telegramId) {
        repository.deleteById(telegramId);
    }
}