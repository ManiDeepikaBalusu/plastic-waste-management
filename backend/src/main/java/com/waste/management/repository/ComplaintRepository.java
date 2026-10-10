package com.waste.management.repository;

import com.waste.management.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    List<Complaint> findByCitizenId(UUID citizenId);
    List<Complaint> findByStatus(String status);
}
