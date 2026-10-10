package com.waste.management.repository;

import com.waste.management.entity.VolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, UUID> {
    Optional<VolunteerProfile> findByUserId(UUID userId);
    List<VolunteerProfile> findByIsAvailableTrue();
}
