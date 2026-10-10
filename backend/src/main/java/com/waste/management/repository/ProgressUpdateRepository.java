package com.waste.management.repository;

import com.waste.management.entity.ProgressUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, UUID> {
    List<ProgressUpdate> findByComplaintIdOrderByCreatedAtDesc(UUID complaintId);
}
