package com.project.ratingservice.repository;

import com.project.ratingservice.model.UserCandidateScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<UserCandidateScore, Long> {

    List<UserCandidateScore> findTop50ByViewerIdOrderByCombinedScoreDesc(Long viewerId);

    Optional<UserCandidateScore> findByViewerIdAndCandidateId(Long viewerId, Long candidateId);
}