package com.project.datingbot.repository;

import com.project.datingbot.entity.RegistrationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends JpaRepository<RegistrationContext, Long> {

}