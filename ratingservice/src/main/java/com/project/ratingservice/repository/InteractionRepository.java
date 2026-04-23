package com.project.ratingservice.repository;

import com.project.ratingservice.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    long countByTargetIdAndType(Long targetId, String type);
}