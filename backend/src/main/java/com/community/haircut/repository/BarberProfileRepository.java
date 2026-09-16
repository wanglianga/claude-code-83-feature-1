package com.community.haircut.repository;

import com.community.haircut.entity.BarberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberProfileRepository extends JpaRepository<BarberProfile, Long> {

    Optional<BarberProfile> findByUserId(Long userId);

    List<BarberProfile> findByActiveTrue();
}
