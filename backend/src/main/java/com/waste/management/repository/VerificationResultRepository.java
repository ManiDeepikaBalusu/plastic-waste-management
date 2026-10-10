package com.waste.management.repository;

import com.waste.management.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, UUID> {
    List<VerificationResult> findByComplaintId(UUID complaintId);
}
