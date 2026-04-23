package com.project.ratingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_candidate_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"viewer_id", "candidate_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCandidateScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long viewerId;
    private Long candidateId;

    private Double primaryScore;
    private Double behavioralScore;
    private Double combinedScore;

    private LocalDateTime updatedAt;
}