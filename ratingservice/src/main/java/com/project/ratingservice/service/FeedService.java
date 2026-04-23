package com.project.ratingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ratingservice.model.User;
import com.project.ratingservice.model.UserCandidateScore;
import com.project.ratingservice.repository.InteractionRepository;
import com.project.ratingservice.repository.ScoreRepository;
import com.project.ratingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final UserRepository userRepo;
    private final InteractionRepository interactionRepo;
    private final ScoreRepository scoreRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public List<Long> getFeed(Long viewerId) {

        String key = "feed:" + viewerId;

        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            try {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
            } catch (Exception e) {
                redisTemplate.delete(key);
            }
        }

        List<User> users = generateFeed(viewerId);

        List<Long> result = users.stream()
                .map(User::getId)
                .toList();

        try {
            redisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(result),
                    Duration.ofMinutes(10)
            );
        } catch (Exception ignored) {}

        return result;
    }

    public List<User> generateFeed(Long userId) {

        User viewer = userRepo.findByTelegramId(userId);

        List<User> candidates = userRepo.findAllCandidatesByFilter(
                viewer.getId(),
                viewer.getPreferences().getMinAge(),
                viewer.getPreferences().getMaxAge(),
                viewer.getPreferences().getGender(),
                viewer.getPreferences().getCity()
        );

        return rank(viewer, candidates);
    }

    public List<User> rank(User me, List<User> candidates) {
        return candidates.stream()
                .sorted((a, b) -> Double.compare(
                        calculateScore(me, b),
                        calculateScore(me, a)
                ))
                .limit(50)
                .toList();
    }

    public double calculateScore(User me, User candidate) {

        double score = 0;

        if (me.getCity().equals(candidate.getCity())) {
            score += 20;
        }

        int diff = Math.abs(me.getAge() - candidate.getAge());
        score += Math.max(0, 20 - diff);

        var canScore = scoreRepo.findByViewerIdAndCandidateId(me.getId(), candidate.getId())
                .orElseGet(() -> createInitialScore(me, candidate));

        score += canScore.getBehavioralScore() * 0.3;
        score += canScore.getCombinedScore() * 0.5;

        return score;
    }

    private UserCandidateScore createInitialScore(User viewer, User candidate) {

        double primary = calculatePrimary(viewer, candidate);
        double behavioral = 0.5;
        double combined = combine(primary, behavioral);

        UserCandidateScore score = new UserCandidateScore();
        score.setViewerId(viewer.getId());
        score.setCandidateId(candidate.getId());
        score.setPrimaryScore(primary);
        score.setBehavioralScore(behavioral);
        score.setCombinedScore(combined);
        score.setUpdatedAt(LocalDateTime.now());

        return scoreRepo.save(score);
    }

    private double calculatePrimary(User viewer, User candidate) {
        double score = 0;

        if (candidate.getAge() >= viewer.getPreferences().getMinAge() &&
                candidate.getAge() <= viewer.getPreferences().getMaxAge()) {
            score += 0.4;
        }

        if (viewer.getCity().equals(candidate.getCity())) {
            score += 0.3;
        }

        if (candidate.getProfileComplete()) {
            score += 0.2;
        }

        return score;
    }

    private double calculateBehavioral(Long candidateId) {
        long likes = interactionRepo.countByTargetIdAndType(candidateId, "LIKE");
        long skips = interactionRepo.countByTargetIdAndType(candidateId, "SKIP");

        if (likes + skips == 0) return 0.5;

        return (double) likes / (likes + skips);
    }

    private double combine(double primary, double behavioral) {
        return primary * 0.7 + behavioral * 0.3;
    }

    private void saveScore(Long viewerId, Long candidateId,
                           double primary, double behavioral, double combined) {

        UserCandidateScore score = scoreRepo
                .findByViewerIdAndCandidateId(viewerId, candidateId)
                .orElse(new UserCandidateScore());

        score.setViewerId(viewerId);
        score.setCandidateId(candidateId);
        score.setPrimaryScore(primary);
        score.setBehavioralScore(behavioral);
        score.setCombinedScore(combined);
        score.setUpdatedAt(LocalDateTime.now());

        scoreRepo.save(score);
    }

    public void onInteraction(Long userId) {
        generateFeed(userId);
    }
}
