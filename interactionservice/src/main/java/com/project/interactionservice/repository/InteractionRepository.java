package com.project.interactionservice.repository;

import com.project.interactionservice.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    long countByTargetIdAndType(Long targetId, String type);

    List<Interaction> findByTargetIdAndTypeOrderByCreatedAtDesc(Long targetId, String type);
}