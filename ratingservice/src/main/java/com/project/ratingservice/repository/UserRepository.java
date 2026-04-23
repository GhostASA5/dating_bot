package com.project.ratingservice.repository;

import com.project.ratingservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.gender = :gender")
    List<User> findAllCandidates(@Param("gender") String gender);

    @Query("""
    SELECT u FROM User u
    WHERE u.id != :meId

    AND u.age BETWEEN :minAge AND :maxAge
    AND (:gender IS NULL OR u.gender = :gender)
    AND (:city IS NULL OR u.city = :city)
    """)
    List<User> findAllCandidatesByFilter(@Param("meId") Long meId,
                                         @Param("minAge") Integer minAge,
                                         @Param("maxAge") Integer maxAge,
                                         @Param("gender") String gender,
                                         @Param("city") String city);

    User findByTelegramId(Long telegramId);
}
