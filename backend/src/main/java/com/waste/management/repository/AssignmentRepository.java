package com.waste.management.repository;

import com.waste.management.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByVolunteerId(UUID volunteerId);
    List<Assignment> findByComplaintId(UUID complaintId);
}
